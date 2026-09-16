# Email templates

## V2 layout contract

Every template whose `EmailMessageTemplate` identifier starts with `V2_` must render through
`layouts/en/v2.hbs`. The shared layout owns the responsive card, organization branding, and footer;
individual templates own only their correspondence-specific content.

`CARE_ENCOUNTER_FOLLOW_UP` also renders through the V2 layout for compatibility with its existing
template identifier. Its sanitized Care Navigator-authored HTML is inserted into the `content` block
before the rendered email is snapshotted as a `FREEFORM` scheduled message.

Each localized V2 `body.hbs` can provide these blocks:

- `preheader`: hidden inbox-preview copy.
- `content`: the central, email-safe table markup for that correspondence.
- `footer`: the default transactional explanation used when the institution has no custom footer.
- `styles` and `mobileStyles`: optional content-specific CSS. Do not add correspondence-specific CSS
  to the shared layout.

The layout consumes these common context fields automatically:

- `platformName`, `institutionId`, and `platformEmailImageUrl` for organization branding.
- `colors.n50`, `colors.n900`, and `colors.p500` from the institution's shared color palette.
- `emailFooterText` for an escaped, institution-specific, single-paragraph footer. When present, it
  replaces the template's `footer` block.
- `privacyPolicyUrl` for the optional Privacy Policy link.

`platformEmailImageUrl` comes from `institution.platform_email_image_url` unless the sending flow
provides `OVERRIDE_PLATFORM_EMAIL_IMAGE_URL`; that per-message override takes precedence.

Minimal V2 body:

```handlebars
{{#partial "preheader"}}Inbox preview text.{{/partial}}

{{#partial "content"}}
	<table width="100%" border="0" cellspacing="0" cellpadding="0" role="presentation">
		<tr>
			<td>Correspondence-specific content</td>
		</tr>
	</table>
{{/partial}}

{{#partial "footer"}}
	Default transactional explanation.
{{/partial}}

{{> layouts/en/v2}}
```

Keep subject copy in the localized `subject.hbs` file. URLs placed in trusted `href` attributes use
triple braces; recipient- or institution-authored plain text uses normal double braces so it remains
HTML-escaped.
