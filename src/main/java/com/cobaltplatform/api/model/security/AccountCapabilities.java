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

package com.cobaltplatform.api.model.security;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AccountCapabilities {
	// Nav items
	// See menu.tsx on frontend
	private boolean viewNavAdminGroupSession;
	private boolean viewNavAdminGroupSessionRequest;
	private boolean viewNavAdminMyContent;
	private boolean viewNavAdminAvailableContent;
	private boolean viewNavAdminCalendar;

	public boolean isViewNavAdminGroupSession() {
		return viewNavAdminGroupSession;
	}

	public void setViewNavAdminGroupSession(boolean viewNavAdminGroupSession) {
		this.viewNavAdminGroupSession = viewNavAdminGroupSession;
	}

	public boolean isViewNavAdminGroupSessionRequest() {
		return viewNavAdminGroupSessionRequest;
	}

	public void setViewNavAdminGroupSessionRequest(boolean viewNavAdminGroupSessionRequest) {
		this.viewNavAdminGroupSessionRequest = viewNavAdminGroupSessionRequest;
	}

	public boolean isViewNavAdminMyContent() {
		return viewNavAdminMyContent;
	}

	public void setViewNavAdminMyContent(boolean viewNavAdminMyContent) {
		this.viewNavAdminMyContent = viewNavAdminMyContent;
	}

	public boolean isViewNavAdminAvailableContent() {
		return viewNavAdminAvailableContent;
	}

	public void setViewNavAdminAvailableContent(boolean viewNavAdminAvailableContent) {
		this.viewNavAdminAvailableContent = viewNavAdminAvailableContent;
	}

	public boolean isViewNavAdminCalendar() {
		return viewNavAdminCalendar;
	}

	public void setViewNavAdminCalendar(boolean viewNavAdminCalendar) {
		this.viewNavAdminCalendar = viewNavAdminCalendar;
	}
}
