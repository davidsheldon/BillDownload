package com.bitclean.billscrape.virginmedia;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.bitclean.billscrape.DefaultOptions;
import com.bitclean.billscrape.Scraper;
import com.bitclean.billscrape.utils.CollectionUtils;
import com.bitclean.billscrape.utils.MyHtmlUnitDriver;
import com.bitclean.billscrape.utils.ElementText;
import com.google.common.collect.Collections2;

/**
 */
public class VirginScraper implements Scraper {
  private final Options config_;

  public VirginScraper(final Options options) {

    config_ = options;
  }

  public void run() {
    WebDriver driver = new MyHtmlUnitDriver();

    driver.get("http://www.virginmedia.com/customers/myaccount/cable-ebill.php");

    BillPage bp = new BillPage(driver);
    SigninPage sp = bp.clickSignIn().signin();

    final CurrentBillsPage currentPage = sp.signin(config_.getUsername(), config_.getPassword());
    currentPage.getAllBills();
    currentPage.previousBills().getAllBills();
  }

  public static class Options extends DefaultOptions {
    public Scraper getInstance() {

      if (StringUtils.isEmpty(password_)) {
        System.err.println("Missing password.");
        throw new IllegalArgumentException("Missing password");
      }
      if (StringUtils.isEmpty(username_)) {
        System.err.println("Missing username.");
        throw new IllegalArgumentException("Missing username");
      }

      return new VirginScraper(this);
    }

  }

  public class BillPage extends PageObject {
    public BillPage(final WebDriver driver) {
      super(driver);
    }

    public MainPage clickSignIn() {
      WebElement link = driver.findElement(By.xpath("//h4[text() = 'eBilling']/following-sibling::a"));
      link.click();
      return new MainPage(driver);
    }
  }

  public class MainPage extends PageObject {
    public MainPage(final WebDriver driver) {
      super(driver);
    }

    public SigninPage signin() {
      driver.findElement(By.linkText("Sign In")).click();
      return new SigninPage(driver);
    }


  }

  public class SigninPage extends PageObject {

    public SigninPage(final WebDriver driver) {
      super(driver);
    }

    public CurrentBillsPage signin(String username, String password) {
      driver.findElement(By.id("EmailAddress")).sendKeys(username);
      final WebElement pin = driver.findElement(By.id("PIN"));

      pin.sendKeys(password);
      pin.findElement(By.xpath("ancestor::form")).findElement(By.className("submit_button")).click();
      return new CurrentBillsPage(driver);
    }

  }

  public class PageWithBills extends PageObject {
    public PageWithBills(final WebDriver driver) {
      super(driver);
    }

    public void getAllBills() {
      final List<WebElement> bills = driver.findElements(By.className("billSummary"));

      // First we get all the bill names.
      for (WebElement bill : bills) {
        showDetails(bill);
      }

      // Then we download each one.
      for (int i = 0; i < bills.size(); i++) {
        downloadBillOnPage(driver, i);
      }
    }

    private void downloadBillOnPage(final WebDriver driver, final int offset) {
      final List<WebElement> bills = driver.findElements(By.className("billSummary"));
      downloadBill(bills.get(offset));
    }

    private void downloadBill(final WebElement bill) {
      Map<String, String> properties = getBillDetails(bill);
      Date date = getBillDate(properties);
      final String accountNumber = properties.get("Your account number");

      File savefile = config_.getSaveFile(date, accountNumber);
      if (savefile == null) {
        return;
      }

      final List<WebElement> elements = bill.findElements(By.tagName("input"));
      for (WebElement element : elements) {
        final String value = element.getAttribute("value");
        if ("Show Me This Bill".equals(value)) {
          config_.verboseLog("Clicking on: " + value);
          element.click();
          break;
        }
      }

      StatementSummaryPage summary = new StatementSummaryPage(driver);

      summary.downloadBill(savefile);

      // Put it back where we started.
      driver.navigate().back();
      driver.navigate().back();
      config_.verboseLog("Back to: " + driver.getTitle());
    }

    private void showDetails(final WebElement bill) {
      Map<String, String> properties = getBillDetails(bill);

      config_.verboseLog("Bill: ");
      for (String s : properties.keySet()) {
        config_.verboseLog(s + ": " + properties.get(s));
      }

      getBillDate(properties);

    }

    private Date getBillDate(final Map<String, String> properties) {
      SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);

      final String dateStr = properties.get("Bill date");
      try {
        return format.parse(dateStr);
      }
      catch (ParseException e) {
        config_.verboseLog("Unable to parse bill date: " + dateStr);
        return null;
      }
    }

    private Map<String, String> getBillDetails(final WebElement bill) {
      final List<WebElement> items = bill.findElement(By.tagName("dl")).findElements(By.xpath("*"));


      Map<String, String> properties = CollectionUtils.mapFromIterable(Collections2.transform(items, new ElementText()));
      return properties;
    }

  }


  public class CurrentBillsPage extends PageWithBills {

    public CurrentBillsPage(final WebDriver driver) {
      super(driver);
    }

    public PreviousBillsPage previousBills() {
      driver.findElement(By.linkText("View previous bills")).click();
      return new PreviousBillsPage(driver);
    }


  }

  public class PreviousBillsPage extends PageWithBills {
    public PreviousBillsPage(final WebDriver driver) {
      super(driver);
    }
  }

  public class StatementSummaryPage extends PageObject {

    public StatementSummaryPage(final WebDriver driver) {
      super(driver);
    }

    public void downloadBill(final File savefile) {
      // Doesn't work, it uses JS
      // driver.findElement(By.partialLinkText("paper bill")).click();
      try {
        final String pdfUrl = new URI(driver.getCurrentUrl()).resolve("GeneratePDF").toString();
        config_.verboseLog("PDF URL: " + pdfUrl);
        ((MyHtmlUnitDriver) driver).saveUrl(savefile, pdfUrl);
      }
      catch (URISyntaxException e) {
        System.err.println("Unable to parse URL.");
      }
      catch (IOException e) {
        config_.log("Unable to save pdf");
      }


    }

  }

  public class PageObject {
    WebDriver driver;

    public PageObject(final WebDriver driver) {
      this.driver = driver;
      config_.verboseLog("On page: " + driver.getTitle());
    }
  }

}
