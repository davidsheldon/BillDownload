package com.bitclean.billscrape.Tmobile;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.bitclean.billscrape.Scraper;
import com.bitclean.billscrape.ScraperDefinition;
import com.bitclean.billscrape.utils.MyHtmlUnitDriver;
import com.gargoylesoftware.htmlunit.RefreshHandler;
import com.gargoylesoftware.htmlunit.WaitingRefreshHandler;

public class TmobileScraper implements Scraper {
  private final Options config_;

  public TmobileScraper(final Options options) {
    config_ = options;
  }

  public void run() {
    MyHtmlUnitDriver driver = new MyHtmlUnitDriver();

    final RefreshHandler oldRefreshHandler = driver.client().getRefreshHandler();
    driver.client().setRefreshHandler(new WaitingRefreshHandler(10));

    driver.get("http://www.t-mobile.co.uk");

    LoginPage page = new FrontPage(driver).login();
    YourAccountPage accountPage = page.login(config_.getUsername(), config_.getPassword());
    accountPage.getBills();
    
    driver.client().setRefreshHandler(oldRefreshHandler);

  }

  public static class Options implements ScraperDefinition {
    boolean overwrite = false;

    private File baseDir = new File("/tmp");

    private String filenamePattern = "{0}-{1}.pdf";

    boolean verbose = false;

    private String password_;

    private String username_;

    boolean quiet;

    public Scraper getInstance() {

      if (StringUtils.isEmpty(password_)) {
        System.err.println("Missing password.");
        throw new IllegalArgumentException("Missing password");
      }
      if (StringUtils.isEmpty(username_)) {
        System.err.println("Missing username.");
        throw new IllegalArgumentException("Missing username");
      }

      return new TmobileScraper(this);
    }

    public String getUsername() {
      return username_;
    }

    public String getPassword() {
      return password_;
    }

    public void setPassword(final String password) {
      password_ = password;
    }

    public void setUsername(final String username) {
      username_ = username;
    }

    private File getFile(final Date date, final String accountNumber) {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

      return new File(baseDir, MessageFormat.format(filenamePattern, accountNumber, dateFormat.format(date)));
    }

    public void verboseLog(final String message) {
      if (verbose) {
        System.err.println(message);
      }
    }

    public void log(final String message) {
      if (!quiet) {
        System.out.println(message);
      }
    }
  }

  private class FrontPage extends PageObject {
    public FrontPage(final WebDriver driver) {
      super(driver);
    }

    public LoginPage login() {
      driver.findElement(By.linkText("Log in")).click();
      return new LoginPage(driver);
    }
  }

  private class LoginPage extends PageObject {
    public LoginPage(final WebDriver driver) {
      super(driver);
    }

    public YourAccountPage login(final String username, final String password) {
      driver.findElement(By.id("username")).sendKeys(username);
      driver.findElement(By.id("password")).sendKeys(password);

      driver.findElement(By.id("mtm-login")).submit();
      return new YourAccountPage(driver);
    }

  }

  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

  private class YourAccountPage extends PageObject {

    public YourAccountPage(final WebDriver driver) {
      super(driver);
    }

    public void getBills() {
      String accountNumber = driver.findElement(By.id("actual-account-number")).getText();
      String phoneNumber = driver.findElement(By.id("actual-phone-number")).getText();
      // Select box listing bills.
      final WebElement billSelect = driver.findElement(By.id("billing-date"));

      final List<WebElement> bills = billSelect.findElements(By.tagName("option"));
      List<String> values = new ArrayList<String>();
      for (WebElement bill : bills) {
        final String dateValue = bill.getAttribute("value");
        config_.verboseLog("Date: " + bill.getText() + " Val: " + dateValue);
        Date date = null;
        try {
          date = dateFormat.parse(dateValue);
        }
        catch (ParseException e) {
          continue;
        }
        final File saveFile = config_.getFile(date, accountNumber);
        if (saveFile.exists() && !config_.overwrite) {
          config_.log("File exists, skipping " + saveFile);
          continue;
        }
        values.add(dateValue);
      }

      for (String bill : values) {
        downloadBill(bill);
      }


    }

    private void downloadBill(final String billDate) {
      final WebElement billSelect = driver.findElement(By.id("billing-date"));
      final List<WebElement> billOptions = billSelect.findElements(By.tagName("option"));
      for (WebElement billOption : billOptions) {
        if (billDate.equals(billOption.getValue())) {
          billOption.setSelected();
          break;
        }
      }
      billSelect.submit();

      new BillsPage(driver).downloadBill(billDate);
    }
  }

  private class BillsPage extends PageObject {
    public BillsPage(final WebDriver driver) {
      super(driver);
    }


    private void downloadBill(final String billDate) {
      final String accountNumber = driver.findElement(By.id("account-number")).getText();
      config_.verboseLog("Account number: " + accountNumber);
      Date date = null;
      try {
        date = dateFormat.parse(billDate);
      }
      catch (ParseException e) {
        return;
      }

      final File saveFile = config_.getFile(date, accountNumber);
      if (saveFile.exists() && !config_.overwrite) {
        config_.log("File exists, skipping " + saveFile);
        return;
      }


      driver.findElement(By.linkText("Download or order a bill")).click(); // On download page.


      driver.findElement(By.xpath("//input[@value='Download']")).click();
      try {
        config_.log("Saving to: " + saveFile);
        ((MyHtmlUnitDriver) driver).saveLastResponse(saveFile);
      }
      catch (IOException e) {
        config_.log("Unable to save pdf");
      }
      driver.navigate().back();
      driver.findElement(By.id("MyT-Mobilehome")).click();
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