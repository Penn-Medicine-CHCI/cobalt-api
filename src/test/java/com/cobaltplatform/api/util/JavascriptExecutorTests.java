/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Originally created at the University of Pennsylvania and Penn Medicine by:
 * Dr. David Asch; Dr. Lisa Bellini; Dr. Cecilia Livesey; Kelley Kugler; and Dr. Matthew Press.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cobaltplatform.api.util;

import org.junit.Test;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class JavascriptExecutorTests {
	@Test
	public void testJavascriptInput() throws JavascriptExecutionException {
		new JavascriptExecutor().execute("""
				if(input.date !== '2022-06-23')
				  throw 'Dates are unequal';
								
				// Quick hack array equality check via JSON...
				if(JSON.stringify(input.stringList) !== JSON.stringify(['a', 'b', 'c']))
				  throw 'String lists are unequal';
				""".trim(), Map.of(
				"stringList", List.of("a", "b", "c"),
				"date", LocalDate.of(2022, 6, 23)
		), TestOutput.class);
	}

	@Test
	public void testJavascriptOutput() throws JavascriptExecutionException {
		TestOutput testOutput = new JavascriptExecutor().execute("""
				output.date = '2022-06-23';
				output.stringList = ['a', 'b', 'c'];
				""", Map.of(), TestOutput.class);

		assertEquals(LocalDate.of(2022, 6, 23), testOutput.getDate());
		assertEquals(List.of("a", "b", "c"), testOutput.getStringList());
	}

	@Test
	public void testJavascriptException() {
		try {
			new JavascriptExecutor().execute("""
					throw 'oops';
					""".trim(), Map.of(), TestOutput.class);
		} catch (JavascriptExecutionException e) {
			// Expected
		}

		try {
			new JavascriptExecutor().execute("""
					x = // syntax error
					""".trim(), Map.of(), TestOutput.class);
		} catch (JavascriptExecutionException e) {
			// Expected
		}
	}

	@NotThreadSafe
	public static class TestOutput {
		@Nullable
		private List<String> stringList;
		@Nullable
		private LocalDate date;

		@Nullable
		public List<String> getStringList() {
			return this.stringList;
		}

		public void setStringList(@Nullable List<String> stringList) {
			this.stringList = stringList;
		}

		@Nullable
		public LocalDate getDate() {
			return this.date;
		}

		public void setDate(@Nullable LocalDate date) {
			this.date = date;
		}
	}
}
