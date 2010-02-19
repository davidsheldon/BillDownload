package com.bitclean.billscrape.utils;

import org.openqa.selenium.WebElement;

import com.google.common.base.Function;

/**
*/
public class ElementText implements Function<WebElement, String> {

public String apply(final WebElement webElement) {
  return webElement.getText();
}
}
