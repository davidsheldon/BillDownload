package com.bitclean.billscrape.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;

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

    final WebResponse response = client().getPage(pdfUrl).getWebResponse();
    download(savefile, response);
  }

  private void download(final File savefile, final WebResponse response) throws IOException {
    savefile.getParentFile().mkdirs();
    final InputStream is = response.getContentAsStream();


    OutputStream os = new FileOutputStream(savefile);
    int i;
    while ((i = is.read()) != -1) {
      os.write(i);
    }
    os.close();
    is.close();
  }

  public void saveLastResponse(final File savefile) throws IOException {
    Page page = lastPage();
    download(savefile, page.getWebResponse());
  }
}
