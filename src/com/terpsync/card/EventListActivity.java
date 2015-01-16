package com.terpsync.card;

import java.util.ArrayList;
import java.util.List;

import com.terpsync.FloatingActionButton;
import com.terpsync.R;
import com.terpsync.parse.EventObject;
import com.terpsync.parse.ParseConstants;
import com.google.android.gms.drive.internal.x;
import com.google.android.gms.internal.ma;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class EventListActivity extends Activity {

	private static final String TAG = "EventListActivity";
	private FloatingActionButton returnFAB, filterFAB, buildingFAB, orgFAB, priceFAB;
	CardListAdapter mAdapter;
	ListView lv; // List for all the event cards
	List<EventObject> fullEventList, filteredEventList;
	AlertDialog.Builder action_builder, delete_builder;
	View view = null;
	boolean isDeleted = false, filterMenuOpen = false, orgFiltered = false,
			buildingFiltered = false;
	int priceFiltered = 0; // 0 = All, 1 = Free, 2 = Paid
	String deletedBuildingName = "";
	String filterType, filterName;
	String buildingFilterName, orgFilterName;
	String[] actionOptions = { "Edit Event", "Delete Event" };

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_list);
		action_builder = new AlertDialog.Builder(this);
		delete_builder = new AlertDialog.Builder(this);
		getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#00A0B0")));
		lv = (ListView) findViewById(R.id.event_list);

		// Determine filter options
		Intent intent = getIntent();
		filterType = intent.getStringExtra("FilterType");
		filterName = intent.getStringExtra(filterType);

		setupFAB();
		determineFilterAndCreateList();
	}

	/**
	 * Decides how to initially filter the list and creates it.
	 * 
	 * List can be filtered by: Organization, Building, or None.
	 */
	private void determineFilterAndCreateList() {
		if (filterType.equals("All")) { // Un-filtered, all events
			getEventsAndCreateList(filterType, "");
		} else if (filterType.equals(ParseConstants.event_org_name)) { // Filter by organization
																		// name
			getEventsAndCreateList(filterType, filterName);
			orgFiltered = true;
			setActionDialog();
		} else if (filterType.equals(ParseConstants.event_location)) { // Filter by building name
			getEventsAndCreateList(filterType, filterName);
			buildingFiltered = true;
		}
	}

	/**
	 * This method gets all the events (either filtered or un-filtered) from the Parse database and
	 * adds them to the CardListAdapater to display.
	 * 
	 * @param filterType
	 *            Determines whether to filter or not. Use "All" to get all events,
	 *            "OrganizationName" to filter by organization, or "BuildingName" to filter by
	 *            building.
	 * @param filterName
	 *            If events are being filtered, this variable specifies the value to filter by.
	 */
	private void getEventsAndCreateList(String filterType, String filterName) {
		// Create the Parse Query object
		ParseQuery<EventObject> eventsQuery = ParseQuery.getQuery(EventObject.class);
		// Sort events by Start Date
		eventsQuery.orderByAscending("StartDate");
		// Checks if events need to be filtered
		if (!filterType.equals("All")) {
			eventsQuery.whereContains(filterType, filterName);
		}
		// Initiate a background thread, retrieve all Event Objects
		eventsQuery.findInBackground(new FindCallback<EventObject>() {
			@Override
			public void done(List<EventObject> events, ParseException e) {
				if (e == null) { // All events were successfully returned
					fullEventList = events;
					mAdapter = new CardListAdapter(getApplicationContext(), R.layout.card, events);
					lv.setAdapter(mAdapter);
				} else { // object retrieval failed throw exception -- fail fast
					e.printStackTrace();
				}
			}
		});
		// filteredEventList = new ArrayList<EventObject>(fullEventList);

		// TODO - Add On-Click Listeners for Cards
		// - if click on card, expand cards (show detailed view w/button to edit/delete)
		// - if click on org, show profile page for org
		// - if click on building, show filtered events by building
	}

	/**
	 * Creates a dialog box to allow the following actions on an event: edit and delete
	 */
	private void setActionDialog() {
		// TODO - change this so it only adds dialog to current user's events
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adaptView, View v, int position, long id) {
				final int pos = position;
				final EventObject x = (EventObject) adaptView.getItemAtPosition(pos);

				// Create alert dialog
				action_builder.setTitle("Please select an option")
						.setItems(actionOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int item) {
								switch (item) {
								case 0: // Edit Event
									Toast.makeText(getBaseContext(), "Implement Editing Event",
											Toast.LENGTH_LONG).show();
									break;

								case 1: // Delete Event
									deletedBuildingName = x.getBuildingName();
									delete_builder
											.setTitle(
													"Delete Event? (Warning: this cannot be undone!)")
											.setPositiveButton("Delete",
													new DialogInterface.OnClickListener() {

														@Override
														public void onClick(DialogInterface dialog,
																int which) {
															isDeleted = true;
															mAdapter.mEventsList.remove(pos);
															mAdapter.notifyDataSetChanged();
															x.deleteInBackground();
														}
													})
											.setNegativeButton("Cancel",
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(DialogInterface dialog,
																int which) {
															dialog.cancel();
														}
													}).create().show();
									break;

								default:
									break;
								}
							}
						}).create().show();
			}
		});
	}

	/**
	 * Sets up the following Floating Action Buttons: returnFAB and filterFAB.
	 */
	private void setupFAB() {
		returnFABListener();
		filterFABListener();
	}

	/**
	 * Creates returnFAB and handles clicks on it: _____________
	 */
	private void returnFABListener() {
		returnFAB = new FloatingActionButton.Builder(this)
				.withDrawable(getResources().getDrawable(R.drawable.ic_action_undo))
				.withButtonColor(Color.RED).withGravity(Gravity.BOTTOM | Gravity.RIGHT)
				.withMargins(0, 0, 16, 16).create();
		returnFAB.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isDeleted) {
					setResult(Activity.RESULT_OK,
							new Intent().putExtra("deleteBuildingName", deletedBuildingName));
				} else {
					setResult(Activity.RESULT_OK);
				}
				finish();
			}
		});
	}

	/**
	 * Creates filterFAB and handles clicks on it: expands/collapses filter type FABs.
	 */
	private void filterFABListener() {
		filterFAB = new FloatingActionButton.Builder(this)
				.withDrawable(getResources().getDrawable(R.drawable.ic_action_filter))
				.withButtonColor(Color.BLUE).withGravity(Gravity.BOTTOM | Gravity.RIGHT)
				.withMargins(0, 0, 16, 86).create();
		filterFAB.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (filterMenuOpen) {
					closeFilterMenu();
				} else {
					openFilterMenu();
				}
				filterMenuOpen = !filterMenuOpen;
			}
		});

	}

	/**
	 * Expands the filter menu showing filter type buttons: buildingFAB, orgFAB, priceFAB
	 */
	private void openFilterMenu() {
		buildingFABListener();
		orgFABListener();
		priceFABListner();
	}

	/**
	 * Collapses the filter menu by hiding the filter type buttons
	 */
	private void closeFilterMenu() {
		buildingFAB.hideFloatingActionButton();
		orgFAB.hideFloatingActionButton();
		priceFAB.hideFloatingActionButton();
		updateAdapter();
	}

	/**
	 * Creates buildingFAB and handles click on it: either showing a popup to choose building to
	 * filter by or un-filtering the list (if filtered)
	 */
	private void buildingFABListener() {
		buildingFAB = new FloatingActionButton.Builder(this)
				.withDrawable(getResources().getDrawable(R.drawable.ic_action_building))
				.withGravity(Gravity.BOTTOM | Gravity.RIGHT).withMargins(0, 0, 226, 86).create();
		setBuildingFABState();
		buildingFAB.hideFloatingActionButton();
		buildingFAB.showFloatingActionButton();
		if (!filterType.equals(ParseConstants.event_location)) { // Don't allow clicks when
																	// pre-filtered
			buildingFAB.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (buildingFiltered) {
						buildingFiltered = false;
						buildingFilterName = "none";
						refilterList();
					} else {
						filterByBuilding();
						buildingFiltered = true;
					}
					setBuildingFABState();
				}
			});
		}
	}

	/**
	 * Creates orgFAB and handles click on it: either showing a popup to choose organization to
	 * filter by or un-filtering the list (if filtered)
	 */
	private void orgFABListener() {
		orgFAB = new FloatingActionButton.Builder(this)
				.withDrawable(getResources().getDrawable(R.drawable.ic_action_crowd))
				.withGravity(Gravity.BOTTOM | Gravity.RIGHT).withMargins(0, 0, 156, 86).create();
		setOrgFABState();
		orgFAB.hideFloatingActionButton();
		orgFAB.showFloatingActionButton();

		if (!filterType.equals(ParseConstants.event_org_name)) { // Don't allow clicks when
																	// pre-filtered
			orgFAB.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (orgFiltered) {
						orgFiltered = false;
						orgFilterName = "none";
						refilterList();
					} else {
						filterByOrganization();
						orgFiltered = true;
					}
					setOrgFABState();
				}
			});
		}
	}

	/**
	 * Creates priceFAB and handles click on it: toggles between free, paid, or all events
	 */
	private void priceFABListner() {
		priceFAB = new FloatingActionButton.Builder(this)
				.withDrawable(getResources().getDrawable(R.drawable.ic_action_paid))
				.withGravity(Gravity.BOTTOM | Gravity.RIGHT).withMargins(0, 0, 86, 86).create();
		setPriceFABState();
		priceFAB.hideFloatingActionButton();
		priceFAB.showFloatingActionButton();

		priceFAB.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (priceFiltered) {
				case 0: // Filtering by free
					priceFiltered++;
					filterByPrice();
					break;
				case 1: // Filtering by paid
					priceFiltered++;
					filterByPrice();
					break;
				case 2: // Unfiltering
					priceFiltered = 0;
					refilterList();
					break;
				}
				setPriceFABState();
			}
		});
	}

	/**
	 * Sets the color of buildingFAB to indicate state.
	 * 
	 * Colored = Filtered, Gray = Unfiltered
	 */

	private void setBuildingFABState() {
		if (buildingFiltered) {
			buildingFAB.setFloatingActionButtonColor(Color.CYAN);
		} else {
			buildingFAB.setFloatingActionButtonColor(Color.GRAY);
		}
	}

	/**
	 * Sets the color of orgFAB to indicate state.
	 * 
	 * Colored = Filtered, Gray = Unfiltered
	 */

	private void setOrgFABState() {
		if (orgFiltered) {
			orgFAB.setFloatingActionButtonColor(Color.YELLOW);
		} else {
			orgFAB.setFloatingActionButtonColor(Color.GRAY);
		}
	}

	/**
	 * Sets the color of priceFAB to indicate state.
	 * 
	 * Colored = Filtered, Gray = Unfiltered
	 */
	private void setPriceFABState() {
		switch (priceFiltered) {
		case 0: // All
			priceFAB.setFloatingActionButtonColor(Color.GRAY);
			priceFAB.setFloatingActionButtonDrawable(getResources().getDrawable(
					R.drawable.ic_action_paid));
			break;
		case 1: // Free
			priceFAB.setFloatingActionButtonColor(Color.RED);
			priceFAB.setFloatingActionButtonDrawable(getResources().getDrawable(
					R.drawable.ic_action_free));
			break;
		case 2: // Paid
			priceFAB.setFloatingActionButtonColor(Color.GREEN);
			priceFAB.setFloatingActionButtonDrawable(getResources().getDrawable(
					R.drawable.ic_action_paid));
			break;
		}
	}

	/**
	 * Gets a list of all buildings in current events, allows user to choose building to filter by.
	 */
	private void filterByBuilding() {
		// Get all buildings in list
		final ArrayList<String> buildingList = mAdapter.getValuesFromFields("building");
		CharSequence[] list = buildingList.toArray(new CharSequence[buildingList.size()]);
		Log.i(TAG, "Number of Buildings found: " + buildingList.size());

		if (buildingList.size() > 0) { // Only filter if events exist
			// Create dialog box to choose building to filter
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Filter by building");
			builder.setItems(list, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					buildingFilterName = "0." + buildingList.get(id);
					Log.i(TAG, "Filtering by *Building: " + buildingList.get(id) + "*");
					mAdapter.getFilter().filter(buildingFilterName);
				}
			});
			builder.setCancelable(false);
			AlertDialog alert = builder.create();
			if (!buildingFiltered) { // do not show dialog box if refiltering
				alert.show();
			} else { // refilter with existing filter
				mAdapter.getFilter().filter(buildingFilterName);
			}
		}
	}

	/**
	 * Gets a list of all organizations in current events, allows user to choose organization to
	 * filter by.
	 */
	private void filterByOrganization() {
		// Get all organizations in list
		final ArrayList<String> orgList = mAdapter.getValuesFromFields("organization");
		CharSequence[] list = orgList.toArray(new CharSequence[orgList.size()]);
		Log.i(TAG, "Number of Organizations found: " + orgList.size());

		if (orgList.size() > 0) { // Only filter if events exist
			// Create dialog box to choose organization to filter
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Filter by organization");
			builder.setItems(list, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					orgFilterName = "1." + orgList.get(id);
					Log.i(TAG, "Filtering by *Organization: " + orgList.get(id) + "*");
					mAdapter.getFilter().filter(orgFilterName);
				}
			});
			builder.setCancelable(false);
			AlertDialog alert = builder.create();
			if (!orgFiltered) { // do not show dialog box if refiltering
				alert.show();
			} else { // refilter with existing filter
				mAdapter.getFilter().filter(orgFilterName);
			}
		}
	}

	/**
	 * Filters by either free or paid events determined using priceFiltered.
	 */
	private void filterByPrice() {
		if (priceFiltered == 1) {
			Log.i(TAG, "Filtering by *Free Events*");
			mAdapter.getFilter().filter("2.FREE"); // Free Event
		} else if (priceFiltered == 2) {
			Log.i(TAG, "Filtering by *Paid Events*");
			mAdapter.getFilter().filter("3.FREE"); // Paid Event
		}
	}

	/**
	 * Resets the adapter's data and refilters the events based on the enabled filter types.
	 */
	private void refilterList() {
		resetAdapter();
		if (!filterType.equals(ParseConstants.event_location) && buildingFiltered) {
			filterByBuilding();
		}
		if (!filterType.equals(ParseConstants.event_org_name) && orgFiltered) {
			filterByOrganization();
		}
		if (priceFiltered > 0) {
			filterByPrice();
		}
		updateAdapter();
	}

	/**
	 * Updates the CardListAdapter with the updated list of events.
	 */
	private void updateAdapter() {
		Log.i(TAG, "updating adapter");
		mAdapter.notifyDataSetChanged();
	}

	/**
	 * Resets the CardListAdapter to the un-filtered data list of events.
	 */
	private void resetAdapter() {
		Log.i(TAG, "resetting adapter");
		mAdapter.resetData();
	}

}
