package com.bitclean.billscrape.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

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

  public void saveUrl(final File savefile, final String pdfUrl) throws IOException {
    savefile.getParentFile().mkdirs();
    
    final InputStream is = client().getPage(pdfUrl).getWebResponse().getContentAsStream();


    OutputStream os = new FileOutputStream(savefile);
    int i;
    while ((i = is.read()) != -1) {
      os.write(i);
    }
    os.close();
    is.close();
  }
}
