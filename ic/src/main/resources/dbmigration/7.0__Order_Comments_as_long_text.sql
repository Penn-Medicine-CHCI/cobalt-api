-- apply changes
alter table ic.referral_order_report alter column order_comments type text using order_comments::text;
