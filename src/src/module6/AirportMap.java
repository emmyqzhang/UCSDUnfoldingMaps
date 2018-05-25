package module6;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import parsing.ParseFeed;
import processing.core.PApplet;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.data.ShapeFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.utils.MapUtils;

/**
 * An applet that shows airports (and routes) on a world map.
 * 
 * @author Adam Setters and the UC San Diego Intermediate Software Development
 *         MOOC team
 * 
 */
public class AirportMap extends PApplet {

	UnfoldingMap map;
	private List<Marker> airportList;
	List<Marker> routeList;

	private Marker lastSelected;
	private Marker lastClicked;

	public void setup() {
		// setting up PAppler
		size(800, 600, OPENGL);

		// setting up map and default events
		map = new UnfoldingMap(this, 50, 50, 750, 550);
		MapUtils.createDefaultEventDispatcher(this, map);

		// get features from airport data
		List<PointFeature> features = ParseFeed.parseAirports(this,
				"airports.dat");

		// hashmap for quicker access when matching with
		// routes
		HashMap<Integer, Marker> airportMap = new HashMap<Integer, Marker>();
		HashMap<Integer, Location> airports = new HashMap<Integer, Location>();

		// create markers from features
		for (PointFeature feature : features) {
			AirportMarker m = new AirportMarker(feature);
			m.setRadius(5);
			// airportList.add(m);

			// put airport in hashmap with OpenFlights unique id for key
			airportMap.put(Integer.parseInt(feature.getId()), m);

			airports.put(Integer.parseInt(feature.getId()),
					feature.getLocation());

		}

		// parse route data
		List<ShapeFeature> routes = ParseFeed.parseRoutes(this, "routes.dat");
		routeList = new ArrayList<Marker>();
		for (ShapeFeature route : routes) {

			// get source and destination airportIds
			int source = Integer.parseInt((String) route.getProperty("source"));
			int dest = Integer.parseInt((String) route
					.getProperty("destination"));

			// get locations for airports on route
			if (airports.containsKey(source) && airports.containsKey(dest)) {
				route.addLocation(airports.get(source));
				route.addLocation(airports.get(dest));

				((AirportMarker) airportMap.get(source)).setHasRoutes(true);
				((AirportMarker) airportMap.get(dest)).setHasRoutes(true);
			}

			SimpleLinesMarker sl = new SimpleLinesMarker(route.getLocations(),
					route.getProperties());

			System.out.println(sl.getProperties());

			sl.setHidden(true);
			routeList.add(sl);
		}

		map.addMarkers(routeList);

		// list for airport markers
		airportList = new ArrayList<Marker>(airportMap.values());
		map.addMarkers(airportList);

	}

	public void draw() {
		background(0);
		map.draw();
		addKey();
	}

	/**
	 * Event handler that gets called automatically when the @ mouse moves.
	 */
	@Override
	public void mouseMoved() {
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;

		}
		selectMarkerIfHover(airportList);
		loop();
	}

	// If there is a marker selected
	private void selectMarkerIfHover(List<Marker> markers) {
		// Abort if there's already a marker selected
		if (lastSelected != null) {
			return;
		}

		for (Marker marker : markers) {
			if (marker.isInside(map, mouseX, mouseY)) {
				lastSelected = marker;
				marker.setSelected(true);
				return;
			}
		}
	}

	/**
	 * The event handler for mouse clicks It will display an airport and its
	 * routes
	 */
	@Override
	public void mouseClicked() {
		if (lastClicked != null) {
			uncheckMarkers();
			lastClicked = null;
		} else if (lastClicked == null) {
			if (mouseButton == LEFT) {
				checkAirportsForLfetClick();
			} else if (mouseButton == RIGHT) {
				checkAirportsForRightClick();
			}
		}
	}

	// Helper method that will check if a airport marker was clicked on
	// and respond appropriately
	private void checkAirportsForLfetClick() {
		// Loop over the airports markers to see if one of them is selected
		Set<Location> tempLocation = new HashSet<Location>();
		for (Marker marker : airportList) {
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker) marker;

				// Hide/Show all the other airports and their routes
				for (Marker mhide : routeList) {
					SimpleLinesMarker simpleLinesMarker = (SimpleLinesMarker) mhide;
					if (!simpleLinesMarker.getLocations().contains(
							lastClicked.getLocation())) {
						simpleLinesMarker.setHidden(true);
					} else {
						simpleLinesMarker.setHidden(false);

						for (Location location : simpleLinesMarker
								.getLocations()) {
							tempLocation.add(location);
						}
					}
				}
				for (Marker mhide : airportList) {
					if (tempLocation.contains(mhide.getLocation())) {
						mhide.setHidden(false);
					} else {
						mhide.setHidden(true);
					}
				}
				if (tempLocation.size() == 0 && lastClicked.isHidden()) {
					lastClicked.setHidden(false);
				}

				return;
			}
		}
	}

	// Helper method that will check if a airport marker was clicked on
	// and respond appropriately
	private void checkAirportsForRightClick() {
		// Loop over the airports markers to see if one of them is selected
		for (Marker marker : airportList) {
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker) marker;

				// Hide all the routes
				for (Marker mhide : routeList) {
					mhide.setHidden(true);
				}

				// Show all the analogous airports.
				for (Marker mhide : airportList) {
					if (((AirportMarker) mhide).getType() == ((AirportMarker) lastClicked)
							.getType()) {
						mhide.setHidden(false);
					} else {
						mhide.setHidden(true);
					}
				}

				return;
			}
		}
	}

	// loop over and make uncheck all marks
	private void uncheckMarkers() {
		for (Marker marker : routeList) {
			marker.setHidden(true);
		}

		for (Marker marker : airportList) {
			marker.setHidden(false);
		}
	}

	// helper method to draw key in GUI
	private void addKey() {
		// Remember you can use Processing's graphics methods here
		fill(255, 250, 240);

		int xbase = 25;
		int ybase = 50;

		rect(xbase, ybase, 170, 110);

		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Airport Key", xbase + 25, ybase + 25);

		fill(color(255, 255, 0));
		ellipse(xbase + 35, ybase + 50, 12, 12);
		fill(color(11));
		ellipse(xbase + 35, ybase + 70, 12, 12);

		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Has routes data", xbase + 50, ybase + 50);
		text("No routes data", xbase + 50, ybase + 70);
	}
}
