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

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.Color.ColorId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ColorValue {
	@Nullable
	private ColorValueId colorValueId;
	@Nullable
	private ColorId colorId;
	@Nullable
	private String name;

	public enum ColorValueId {
		N0,
		N50,
		N75,
		N100,
		N300,
		N500,
		N700,
		N900,

		P50,
		P100,
		P300,
		P500,
		P700,
		P900,

		A50,
		A100,
		A300,
		A500,
		A700,
		A900,

		D50,
		D100,
		D300,
		D500,
		D700,
		D900,

		W50,
		W100,
		W300,
		W500,
		W700,
		W900,

		S50,
		S100,
		S300,
		S500,
		S700,
		S900,

		I50,
		I100,
		I300,
		I500,
		I700,
		I900,

		T50,
		T100,
		T300,
		T500,
		T700,
		T900
	}

	@Override
	public String toString() {
		return format("%s{colorValueId=%s, colorId=%s, name=%s}", getClass().getSimpleName(),
				getColorValueId(), getColorId(), getName());
	}

	@Nullable
	public ColorValueId getColorValueId() {
		return this.colorValueId;
	}

	public void setColorValueId(@Nullable ColorValueId colorValueId) {
		this.colorValueId = colorValueId;
	}

	@Nullable
	public ColorId getColorId() {
		return this.colorId;
	}

	public void setColorId(@Nullable ColorId colorId) {
		this.colorId = colorId;
	}

	@Nullable
	public String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}
}