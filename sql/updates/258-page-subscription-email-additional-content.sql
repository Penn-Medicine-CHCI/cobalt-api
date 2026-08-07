BEGIN;
SELECT _v.register_patch('258-page-subscription-email-additional-content', NULL, NULL);

-- Optional, trusted HTML appended to page-subscription emails.  The surrounding
-- divider, spacing, and email-safe table structure are owned by the template.
ALTER TABLE page_group
  ADD COLUMN subscription_email_additional_content_html TEXT;

-- Example: add a custom section to Parents @ Penn Medicine subscription emails:

/*
UPDATE page_group
SET subscription_email_additional_content_html = $html$
<table width="100%" border="0" cellspacing="0" cellpadding="0" role="presentation">
  <tr>
    <td align="left" class="em_h2" style="font-family:'SF Pro Text', 'Segoe UI', 'Helvetica Neue', Arial, sans-serif; font-size:28px; line-height:40px; font-weight:700; color:#292827;">
      Share Your Wisdom With Fellow Parents @ Penn Medicine
    </td>
  </tr>
  <tr>
    <td height="16" style="height:16px; line-height:16px; font-size:1px;">&nbsp;</td>
  </tr>
  <tr>
    <td align="left" class="em_body_copy" style="font-family:'SF Pro Text', 'Segoe UI', 'Helvetica Neue', Arial, sans-serif; font-size:16px; line-height:24px; color:#292827;">
      Every parenting journey is unique, but we learn so much when we share our experiences. Whether it&rsquo;s a bedtime routine hack, a trick for picky eaters, or a way to find calm during a hectic day, your insight could be exactly what another parent needs to hear. Fill out <a href="https://URL_GOES_HERE" target="_blank" style="color:#2C4F82; text-decoration:underline;">this form</a> to share your tips and tricks with fellow parents. Tips will be shared anonymously on the Penn Cobalt Parents @ Penn Medicine page.
    </td>
  </tr>
  <tr>
    <td height="8" style="height:8px; line-height:8px; font-size:1px;">&nbsp;</td>
  </tr>
</table>
$html$
WHERE page_group_id = 'XXXX';
*/

COMMIT;
