/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.pipeline.impl.testdata;

import java.util.List;

/**
 * Java objects that match the structure of Stefan Goessner's little store, which sells several books, but only one
 * bike.
 * @see "http://goessner.net/articles/JsonPath/"
 */
public class BooksDocument {

  private Store store;

  public Store getStore() {
    return this.store;
  }

  public void setStore(Store store) {
    this.store = store;
  }

  public static class Store {

    private List<Book> book;
    private Bicycle bicycle;

    public List<Book> getBook() {
      return this.book;
    }

    public void setBook(List<Book> book) {
      this.book = book;
    }

    public Bicycle getBicycle() {
      return this.bicycle;
    }

    public void setBicycle(Bicycle bicycle) {
      this.bicycle = bicycle;
    }
  }

  public static class Book {

    private String category;
    private String author;
    private String title;
    private String isbn;
    private Double price;

    public String getCategory() {
      return this.category;
    }

    public void setCategory(String category) {
      this.category = category;
    }

    public String getAuthor() {
      return this.author;
    }

    public void setAuthor(String author) {
      this.author = author;
    }

    public String getTitle() {
      return this.title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getIsbn() {
      return this.isbn;
    }

    public void setIsbn(String isbn) {
      this.isbn = isbn;
    }

    public Double getPrice() {
      return this.price;
    }

    public void setPrice(Double price) {
      this.price = price;
    }
  }

  public static class Bicycle {

    private String color;
    private Double price;

    public String getColor() {
      return this.color;
    }

    public void setColor(String color) {
      this.color = color;
    }

    public Double getPrice() {
      return this.price;
    }

    public void setPrice(Double price) {
      this.price = price;
    }
  }

}
