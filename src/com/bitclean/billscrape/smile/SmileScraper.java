package com.bitclean.billscrape.smile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.bitclean.billscrape.DefaultOptions;
import com.bitclean.billscrape.Scraper;
import com.bitclean.billscrape.utils.ElementText;
import com.bitclean.billscrape.utils.MyHtmlUnitDriver;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;

/**
 * Downloads monthly statements as CSV
 */
public class SmileScraper implements Scraper {
  private final Options config_;

  public SmileScraper(final Options options) {

    config_ = options;
  }

  private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);


  public void run() {
    WebDriver driver = new MyHtmlUnitDriver();

    driver.get("https://welcome23.smile.co.uk/SmileWeb/start.do");

    LoginPage bp = new LoginPage(driver);
    final SmileScraper.FirstPage homepage = bp.login(config_);
    final SmileScraper.Previous previous = homepage.clickAccount("current account").previousStatements();
    final List<String> statementList = previous.getStatementList();

    for (int i = 0; i < Math.min(statementList.size(), 12); i++) {
      String date = statementList.get(i);
      final Date d = getDate(date);
      final File saveFile = config_.getSaveFile(d, config_.accountNumber);
      if (d != null && saveFile != null) {
        previous.getStatement(date);
      }
    }
  }

  private Date getDate(final String date) {
    try {
      return dateFormat.parse(date);
    }
    catch (ParseException e) {
      System.err.println("Unable to parse: " + date);
    }
    return null;
  }

  public static class Options extends DefaultOptions {
    String sortcode;

    String accountNumber;

    String passCode;

    String lastSchool;

    String firstSchool;

    String birthPlace;

    String memorableDate;

    String memorableName;

    public Scraper getInstance() {

      if (StringUtils.isEmpty(sortcode)) {
        System.err.println("Missing sortcode.");
        throw new IllegalArgumentException("Missing sortcode.");
      }
      if (StringUtils.isEmpty(accountNumber)) {
        System.err.println("Missing accountNumber.");
        throw new IllegalArgumentException("Missing accountNumber");
      }
      if (StringUtils.isEmpty(passCode)) {
        System.err.println("Missing pass code.");
        throw new IllegalArgumentException("Missing pass code.");
      }

      return new SmileScraper(this);
    }
  }

  private static By okButtonOnForm(String name) {
    return By.xpath("//form[@name='" + name + "']//input[@name='ok']");
  }

  private static String labelFor(WebDriver driver, String field) {
    return driver.findElement(By.xpath("//label[@for='" + field + "']")).getText();
  }

  private class LoginPage extends PageObject {
    public LoginPage(final WebDriver driver) {
      super(driver);
    }

    public FirstPage login(Options o) {
      driver.findElement(By.id("sortcode")).sendKeys(o.sortcode);
      driver.findElement(By.id("accountnumber")).sendKeys(o.accountNumber);
      WebElement okButton = driver.findElement(okButtonOnForm("loginForm"));
      okButton.click();

      String[] fields = new String[] { "firstPassCodeDigit", "secondPassCodeDigit" };
      String[] ordinals = new String[] { "first", "second", "third", "fourth" };

      for (String field : fields) {
        String label = labelFor(driver, field);
        Character digit = null;
        final String ord = label.split(" ")[0];
        for (int j = 0; j < ordinals.length; j++) {
          String ordinal = ordinals[j];
          if (ord.equalsIgnoreCase(ordinal)) {
            digit = o.passCode.charAt(j);
          }
        }
        if (digit == null) {
          o.log("Unable to work out which character is '" + ord + "'");
        }
        System.err.println("Field: " + field + " " + ord + " " + digit);
        final WebElement element = driver.findElement(By.id(field)).findElements(By.tagName("option")).get(digit - '0' + 1);
        element.click();
      }

      okButton = driver.findElement(okButtonOnForm("passCodeForm"));
      okButton.click();

//      ((HtmlPage)((MyHtmlUnitDriver)driver).client().getCurrentWindow().getEnclosedPage()).getFormByName("passCodeForm").setActionAttribute("http://localhost:8000/");
      final WebElement field = driver.findElement(By.xpath("//input[@type='password']"));

      final String requested = field.getAttribute("name");
      o.verboseLog("SPI: " + requested);
      if ("lastSchool".equals(requested)) {
        field.sendKeys(o.lastSchool);
      }
      if ("firstSchool".equals(requested)) {
        field.sendKeys(o.firstSchool);
      }
      if ("birthPlace".equals(requested)) {
        field.sendKeys(o.birthPlace);
      }
      if ("memorableName".equals(requested)) {
        field.sendKeys(o.memorableName);
      }
      if ("memorableDay".equals(requested)) {
        final String[] vals = o.memorableDate.split("-");
        field.sendKeys(vals[0]);
        driver.findElement(By.xpath("//input[@name='memorableMonth']")).sendKeys(vals[1]);
        driver.findElement(By.xpath("//input[@name='memorableYear']")).sendKeys(vals[2]);
      }


      okButton = driver.findElement(okButtonOnForm("loginSpiForm"));
      okButton.click();
      return new FirstPage(driver);
    }

  }

  private class FirstPage extends PageObject {
    public FirstPage(final WebDriver driver) {
      super(driver);
    }

    public AccountPage clickAccount(String name) {
      driver.findElement(By.linkText(name)).click();
      return new AccountPage(driver);
    }
  }

  private class AccountPage extends PageObject {

    public AccountPage(final WebDriver driver) {
      super(driver);
    }

    public Previous previousStatements() {
      driver.findElement(By.xpath("//a[@title = 'view previous statements']")).click();
      return new Previous(driver);
    }
  }

  private class Previous extends PageObject {

    public Previous(final WebDriver driver) {
      super(driver);
    }


    public void getStatement(String date) {
      driver.findElement(By.linkText(date)).click();
      final File saveFile = config_.getSaveFile(getDate(date), config_.accountNumber);

      dumpSummaryTable(saveFile);
      driver.findElement(By.xpath("//a[@title = 'view previous statememts']")).click();
    }

    private void dumpSummaryTable(final File saveFile) {
      PrintWriter writer = null;
      try {
        writer = new PrintWriter(new FileWriter(saveFile));
        WebElement table = driver.findElement(By.xpath("//table[@class='summaryTable']"));

        final List<WebElement> items = table.findElements(By.xpath(".//tr"));
        for (WebElement row : items) {
          writer.println(tableToCSV(row, "td"));
        }
      }
      catch (IOException e) {
        System.err.println("Error writing to " + saveFile + " " + e);
      }
      finally {
        if (writer != null) {
          writer.close();
        }
      }
    }

    private String tableToCSV(final WebElement row, final String elements) {
      final List<WebElement> tds = row.findElements(By.tagName(elements));
      final Collection<String> rowData = Collections2.transform(tds, new ElementText());
      return Joiner.on(",").join(rowData);
    }

    public List<String> getStatementList() {
      WebElement table = driver.findElement(By.xpath("//table[@class='summaryTable']"));

      final List<WebElement> items = table.findElements(By.xpath(".//tr"));
      final List<String> ret = new ArrayList<String>(items.size());
      for (WebElement row : items) {
        final List<WebElement> trs = row.findElements(By.tagName("td"));
        if (! trs.isEmpty()) {
          ret.add(trs.get(1).getText());
        }
      }
      return ret;
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
