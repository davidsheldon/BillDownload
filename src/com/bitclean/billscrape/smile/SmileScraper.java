package com.bitclean.billscrape.smile;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.bitclean.billscrape.DefaultOptions;
import com.bitclean.billscrape.Scraper;
import com.bitclean.billscrape.utils.MyHtmlUnitDriver;

/**
 * Downloads monthly statements as CSV
 */
public class SmileScraper implements Scraper {
  private final Options config_;

  public SmileScraper(final Options options) {

    config_ = options;
  }

  public void run() {
    WebDriver driver = new MyHtmlUnitDriver();

    driver.get("https://welcome23.smile.co.uk/SmileWeb/start.do");

    LoginPage bp = new LoginPage(driver);
    final SmileScraper.FirstPage homepage = bp.login(config_);
    homepage.clickAccount("current account").previousStatements();
  }

  public static class Options extends DefaultOptions {
    String sortcode;

    String accountNumber;

    String passCode;

    String lastSchool;

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

    public void setPassCode(final String passCode) {
      this.passCode = passCode;
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

      for (int i = 0; i < fields.length; i++) {
        String field = fields[i];
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
        System.err.println(element);
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
  }

  public class PageObject {
    WebDriver driver;

    public PageObject(final WebDriver driver) {
      this.driver = driver;
      config_.verboseLog("On page: " + driver.getTitle());
    }
  }
}
