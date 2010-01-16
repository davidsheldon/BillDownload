package com.bitclean.billscrape.utils;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.gargoylesoftware.htmlunit.WebClient;

/**
   * Nasty hack to expose the underlying web client. We want to be able to download the PDF
 * in this client without affecting the WebDriver or using the nasty javascript that the 
 * site uses.
 */
public class MyHtmlUnitDriver extends HtmlUnitDriver {
  public WebClient client() {
    return getWebClient();
  }
}
