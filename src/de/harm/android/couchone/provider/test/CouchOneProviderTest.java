package de.harm.android.couchone.provider.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;
import android.util.Log;
import de.harm.android.couchone.common.CouchConstants;
import de.harm.android.couchone.provider.Provider;

public class CouchOneProviderTest extends ProviderTestCase2<Provider> {

	private MockContentResolver mockResolver;
	private Uri insertedUri;

	public CouchOneProviderTest() {
		super(Provider.class, CouchConstants.AUTHORITY);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mockResolver = getMockContentResolver();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		mockResolver = null;

		// clean the old db
		// getContext().getDatabasePath("test.messages.db").delete();
	}

	public void testConnection() {
		Cursor cursor = mockResolver.query(CouchConstants.CONTENT_URI, null,
				null, null, null);

		boolean connected = false;
		cursor.moveToFirst();
		if (!cursor.isBeforeFirst()) {
			for (int i = 0; i < cursor.getColumnCount(); i++) {
				String col = cursor.getColumnName(i);
				if (col.equals("db_name")) {
					String val = cursor.getString(i);
					Log.e("HarmsProviderTest", "testConnection-1: " + val);
					if (val.equals(CouchConstants.DB_Name)) {
						connected = true;
						break;
					}
				}
				Log.e("HarmsProviderTest",
						"testConnection-2 (should not be reached!!): " + col);
			}
		}
		assertTrue("Connection failed! Should be " + CouchConstants.DB_Name,
				connected);
	}

	public void testGetType() {
		String type = mockResolver.getType(CouchConstants.CONTENT_URI);
		Log.e("HarmsProviderTest", "testGetType: " + type);
		assertEquals(CouchConstants.COLLECTION_TYPE, type);
	}

	public void testInsert() {
		Log.e("HarmsProviderTest", "testInsert-1, uri used for insert: "
				+ CouchConstants.CONTENT_URI.toString());

		// insert
		ContentValues values = new ContentValues();
		values.put(CouchConstants.Contact.FIRSTNAME, "Lüder");
		values.put(CouchConstants.Contact.LASTNAME, "Duda");

		Uri uri = mockResolver.insert(CouchConstants.CONTENT_URI, values);
		Log.e("HarmsProviderTest", "testInsert-2, returned uri after insert: "
				+ uri.toString());
		assertFalse(
				"An insert operation should return a uri including the id.",
				uri.getLastPathSegment().equals(CouchConstants.DB_Name));

		// uri is used for further tests
		insertedUri = Uri.withAppendedPath(CouchConstants.CONTENT_URI,
				uri.getLastPathSegment());
		Log.e("HarmsProviderTest",
				"testInsert-2, create Uri: " + insertedUri.toString());

		testQuery();
		testUpdate();
		testDelete();
	}

	private void testQuery() {
		// query for name vs. testUpdate()
		Cursor cursor = mockResolver.query(CouchConstants.CONTENT_URI, null,
				CouchConstants.VIEW_Default, new String[] { "Lüder" }, null);

		// as a view is used to query the database, the (one or many) results
		// are in the JSON field "key" and "value"
		boolean result = this.query(cursor, "LüderDuda", "key", "value");
		Log.e("HarmsProviderTest", "testQuery: " + result);
		assertTrue("Should jave been 'LüderDuda'", result);
	}

	private void testUpdate() {
		// query and update
		// pass only values to be updated! The Provider loads all data,
		// changes
		// the values and rewrites everything again
		ContentValues values = new ContentValues();
		values.put(CouchConstants.Contact.FIRSTNAME, "Gert Müller");
		values.put(CouchConstants.Contact.LASTNAME, "Friedrichs");

		int success = mockResolver.update(insertedUri, values, null, null);
		Log.e("HarmsProviderTest", "testUpdate-1: " + success);
		assertEquals(1, success);

		// query to verify update and delete
		// query for uri vs. testQuery()
		Cursor cursor = mockResolver.query(insertedUri, null, null, null, null);

		// as one specific JSON document is returned, the JSON field should be
		// according to "CouchConstants.Contact"
		boolean result = this.query(cursor, "Gert MüllerFriedrichs",
				CouchConstants.Contact.FIRSTNAME,
				CouchConstants.Contact.LASTNAME);
		Log.e("HarmsProviderTest", "testUpdate-2: " + result);
		assertTrue("Should have been 'Gert MüllerFriedrichs'", result);
	}

	private void testDelete() {
		Log.e("HarmsProviderTest", "testDelete-1, use created Uri: "
				+ insertedUri.toString());
		int success = mockResolver.delete(insertedUri, null, null);
		Log.e("HarmsProviderTest", "testDelete: " + success);
		assertEquals(1, success);
	}

	private boolean query(Cursor cursor, String query, String key, String value) {
		Log.e("HarmsProviderTest", "private query method: " + query);

		boolean result = false;

		cursor.moveToFirst();
		if (!cursor.isBeforeFirst()) {
			do {
				String fn = "";
				String ln = "";
				for (int i = 0; i < cursor.getColumnCount(); i++) {
					if (cursor.getColumnName(i).equals(key)) {
						fn = cursor.getString(i);
					} else if (cursor.getColumnName(i).equals(value)) {
						ln = cursor.getString(i);
					}

					if (query.equals(fn + ln)) {
						result = true;
						break;
					}
				}
			} while (cursor.moveToNext());
		}
		cursor.close();

		return result;
	}
}
