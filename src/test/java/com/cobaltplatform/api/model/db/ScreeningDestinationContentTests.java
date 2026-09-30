package com.cobaltplatform.api.model.db;

import com.pyranid.DatabaseType;
import com.pyranid.DefaultInstanceProvider;
import com.pyranid.DefaultResultSetMapper;
import org.junit.Test;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import java.sql.Types;

import static org.junit.Assert.assertEquals;

public class ScreeningDestinationContentTests {
	@Test
	public void databaseRowContainsEditableEligibilityExitContent() throws Exception {
		RowSetMetaDataImpl metadata = new RowSetMetaDataImpl();
		metadata.setColumnCount(4);
		String[] columns = { "title", "message", "contact_name", "contact_phone" };
		for (int index = 0; index < columns.length; index++) {
			metadata.setColumnName(index + 1, columns[index]);
			metadata.setColumnLabel(index + 1, columns[index]);
			metadata.setColumnType(index + 1, Types.VARCHAR);
		}

		try (CachedRowSet row = RowSetProvider.newFactory().createCachedRowSet()) {
			row.setMetaData(metadata);
			row.moveToInsertRow();
			row.updateString(1, "EASE Clinic eligibility");
			row.updateString(2, "This pilot is for PAH employees only.");
			row.updateString(3, "Donna Campo");
			row.updateString(4, "215-829-7052");
			row.insertRow();
			row.moveToCurrentRow();
			row.beforeFirst();
			row.next();

			ScreeningDestinationContent content = new DefaultResultSetMapper(
					DatabaseType.GENERIC, new DefaultInstanceProvider())
					.map(row, ScreeningDestinationContent.class);

			assertEquals("EASE Clinic eligibility", content.getTitle());
			assertEquals("This pilot is for PAH employees only.", content.getMessage());
			assertEquals("Donna Campo", content.getContactName());
			assertEquals("215-829-7052", content.getContactPhone());
		}
	}
}
