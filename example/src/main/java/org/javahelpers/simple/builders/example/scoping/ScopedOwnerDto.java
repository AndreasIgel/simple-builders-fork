/*
 * MIT License
 *
 * Copyright (c) 2026 Andreas Igel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.javahelpers.simple.builders.example.scoping;

import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
import org.javahelpers.simple.builders.example.SponsorDto;
import org.javahelpers.simple.builders.example.library.LibraryHelperDto;

/**
 * Demonstrates generation-scope, usage-scope, and out-of-scope builder decisions.
 *
 * <p>{@link TrustedHelperDto} gets a builder consumer, while {@link LibraryHelperDto} and
 * {@link SponsorDto} get plain setters because their builders are unavailable or out of scope.
 */
@SimpleBuilder(
    options =
        @SimpleBuilder.Options(
            builderGenerationPackages = "org.javahelpers.simple.builders.example.scoping",
            builderUsagePackages = "org.javahelpers.simple.builders.example.library"))
public class ScopedOwnerDto {
  private TrustedHelperDto trusted;
  private LibraryHelperDto library;
  private SponsorDto sponsor;

  public TrustedHelperDto getTrusted() {
    return trusted;
  }

  public void setTrusted(TrustedHelperDto trusted) {
    this.trusted = trusted;
  }

  public LibraryHelperDto getLibrary() {
    return library;
  }

  public void setLibrary(LibraryHelperDto library) {
    this.library = library;
  }

  public SponsorDto getSponsor() {
    return sponsor;
  }

  public void setSponsor(SponsorDto sponsor) {
    this.sponsor = sponsor;
  }
}
