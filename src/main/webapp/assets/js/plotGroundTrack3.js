/**
 * 
 * plotGroundTrack3.js
 * 
 * This JavaScript is for groundTrack.html. groundTrack.html is for Android
 * application GNSSFinder. The application's Map function will call by using
 * following URL "https://braincopy.org/tlews/app/groundTrack.html?***=***"
 * 
 * OpenLayers API ver. 5.3.0
 * 
 * Hiroaki Tateshita 
 *  
 * version 0.5.0
 * 
 */



var map;
var gnssString = "JE";
var url_DateTime = "2014-03-01_00:00:00";
var update_timeout = null;
const url_string = "http://localhost:8080";
//const url_string = "https://braincopy.org";

/** 
 * two dimensional array, the number of trackCoordinatesArray[] is the number of
 * satellite, the number of trackCoordinatesArray[][] is the number of the data
 * of each satellite.
 * value also is string array, whose number is 4: Lat, Lon, Date, Time
 */
var trackCoordinatesArray = new Array();

/*
 * trackLine is PolyLine of each satellite. the number of this trackLineArray is
 * the number of satellite.
 */
var trackLineArray = new Array();

/*
 * this array is Marker including satellite image. so the number of this array
 * is the number of satellite.
 */
var markerArray = new Array();

/*
 * this array of Satellite Object is data from satellite database from text file
 */
var satArray = new Array();

/*
 * this array is data (the object of the Satellite class) 
 * from satellite database from text file "satelliteDataBase.txt".
 */
var satNo = new Array();

var isDrawn = false;

/**
 * Icon Array
 */
let marker_array = [];

function initialize() {

	/* Initializing track coordinates array */

	loadSatellite();


	/* Setting Current date and time */
	var currentDateTime = new Date();
	var dateStr = "";
	var timeStr = "";
	dateStr = currentDateTime.getUTCFullYear() + "-";
	if (currentDateTime.getUTCMonth() < 9) {
		dateStr += "0";
	}
	dateStr += (currentDateTime.getUTCMonth() + 1) + "-";
	if (currentDateTime.getUTCDate() < 10) {
		dateStr += "0";
	}
	dateStr += currentDateTime.getUTCDate();
	$('#datepicker').val(dateStr);

	if (currentDateTime.getUTCHours() < 10) {
		timeStr = "0";
	}
	timeStr += currentDateTime.getUTCHours() + ":";
	if (currentDateTime.getUTCMinutes() < 10) {
		timeStr += "0";
	}
	timeStr += currentDateTime.getUTCMinutes() + ":";
	if (currentDateTime.getUTCSeconds() < 10) {
		timeStr += "0";
	}
	timeStr += currentDateTime.getUTCSeconds();
	$('#timepicker').val(timeStr);

	var url = location.href;
	params = url.split("?");
	paramms = params[1].split("=");

	/*
	 * input check
	 */
	if (!paramms[1].match(/^[E-R]+$/)) {
		paramms[1] = "25544";
		// alert("OK: "+paramms[1]);
	}

	if (!isDrawn) {
		startPlot(paramms[1]);
		isDrawn = true;
	}


}

/**
 * 
 * @param {*} iss_cat_id 
 */
function startPlot(iss_cat_id) {
	$("#loading")
		.append(
			'<p style="font-family:arial;color:red;">now loading <img src="assets/images/loading.gif"></p>');

	update_timeout = setTimeout(function () {
		//alert("here click event #2");
		var url_Date_temp = $('#datepicker').val();
		if (url_Date_temp != "") {
			var url_Time_temp = $('#timepicker').val();
			if (url_Time_temp != "") {
				url_DateTime = url_Date_temp + "_" + url_Time_temp;
			}
		}
		var url = url_string + "/tlews/app/groundTrack?" + "dateTime="
			+ url_DateTime + "&norad_cat_id=" + iss_cat_id
			+ "&format=jsonp&term=5400&step=60";
		load_src(url);
	}, 200);
}

window.callback = function (data) {
	createAndDrawTrackCoordinateArray(data.values);
};

function load_src(url) {
	var script = document.createElement('script');
	script.src = url;
	document.body.appendChild(script);
}

function colorString(_value) {
	return '#FF4040';
}


/**
 * class definition for satellite.
 */
function Satellite(_catNo, _rnxStr, _imgStr, _description) {
	this.catNo = _catNo;
	this.rnxStr = _rnxStr;
	this.imgStr = _imgStr;
	this.description = _description;
}

/**
 * load satellite data from text file. output is array of Satellite objects.
 * This method contains initialization of trackCoordinatesArray and satArray.
 */
function loadSatellite() {

	let httpReq = new XMLHttpRequest();
	httpReq.onreadystatechange = function callback_inRoadSatDB() {
		var lines = new Array();
		if (httpReq.readyState == 4 && httpReq.status == 200) {
			// road database
			lines = httpReq.responseText.split("\n", 50);
			ele_line = new Array();
			lines.forEach(function (ele, index, _array) {
				ele_line = ele.split("\t", 5);
				satArray[index] = new Satellite(ele_line[0], ele_line[1],
					ele_line[2], ele_line[3]);
			});
			for (var i = 0; i < satArray.length; i++) {
				trackCoordinatesArray[i] = new Array();
			}
			for (var i = 0; i < satArray.length; i++) {
				satNo[i] = 0;
			}
		}
	};
	const url =
		//'http://localhost:8080/tlews/res/satelliteDataBase.txt';
		'http://127.0.0.1:5501/src/main/webapp/assets/satelliteDataBase.txt';
	//  'https://braincopy.org/WebContent/assets/satelliteDataBase.txt';
	httpReq.open("GET", url, true);
	httpReq.send(null);
}

function convertCoordinate(longitude, latitude) {
	return ol.proj.transform([longitude, latitude], "EPSG:4326", "EPSG:900913");
}

/**
 * 
 * @param {*} ele_sat is should be Satellite object
 */
function satelliteStyle(ele_sat) {
	let src_str = '';
	if (ele_sat.imgStr == "qzs-1") {
		src_str = 'assets/images/qzs-1.png';
	} else if (ele_sat.imgStr == "ISS") {
		src_str = 'assets/images/iss.png';
	}
	return new ol.style.Style({
		image: new ol.style.Icon({
			src: src_str,
			scale: 0.3
		})
	});
}



/**
 * 
 * @param values are the data array from json data of tlews/groundTrack web api.
 * 
 */
function createAndDrawTrackCoordinateArray(values) {

	/*
	const test_ele = [
		[169.719785, 53.923339],
		[-178.87634, 55.489269],
		[171.418013, 56.617763],
		[-162.138846, 58.298914]];
*/

	/**
	 * trackCoordinatesArray will be updated in the 
	 * createTrackDoordinateArray function.
	 */
	values.forEach(createTrackCoordinateArray);

	trackCoordinatesArray.forEach(function (ele, index, _array) {
		if (ele.length > 0) {//ele is an array of lat, lon of each satellite.
			let lineStrings = new ol.geom.MultiLineString([]); // line instance of the path
			let lineStrArray = new Array();                    // line data array as lineString format [[pt0,pt1],[pt1,pt2],[pt2,pt3],....]
			let lineStrArray2 = new Array();// lineStrArray will be divided on latitude 180 and -180 deg.

			/*
			* groundTrackFeatures function is defined in "greatCircle.js"
			*/
			//lineStrArray2 = groundTrackFeatures(test_ele);
			lineStrArray2 = groundTrackFeatures(ele);

			lineStrArray2.forEach(function (val, _i) {
				lineStrArray.push(val);//val is a coordinate array divided on lat +/-180
				lineStrings.setCoordinates(lineStrArray);//lineStrings is OpenLayer object for line
				trackLineArray.push(new ol.Feature(lineStrings.transform('EPSG:4326', 'EPSG:3857')));
			});

			/*
			* in ISS case, index should be only 0.
			*  in this part, set a style including ISS icon image at the 1st point of the lat lon array.
			* create marker
			* ele is an array of lat, lon of each satellite.
			*  so _val is an array: [lat, lon].
			*/
			ele.forEach(function (_val, _i) {
				let tmp_marker = null;
				if (_i == 0) {
					tmp_marker = new ol.Feature({
						geometry: new ol.geom.Point(convertCoordinate(_val[0], _val[1])),
						name: satArray[index].description,
						id: satArray[index].catNo,
						date: _val[2],
						time: _val[3]
					});
					tmp_marker.setStyle(satelliteStyle(satArray[index]));
				} else {
					tmp_marker = new ol.Feature({
						geometry: new ol.geom.Point(convertCoordinate(_val[0], _val[1])),
						name: satArray[index].description,
						id: satArray[index].catNo,
						date: _val[2],
						time: _val[3]
					});
					tmp_marker.setStyle(new ol.style.Style({
						image: new ol.style.Circle({
							radius: 5,
							fill: new ol.style.Fill({
								color: 'blue'
							})
						})
					}));
				}
				marker_array.push(tmp_marker);
			});

		}

	});

	let markerSource = new ol.source.Vector({
		features: marker_array
	});

	let rabelLayer = new ol.layer.Vector({
		source: markerSource
	});

	const osmLayer = new ol.layer.Tile({
		source: new ol.source.OSM()
	});

	//For GSI Tile
	const gsiLayer = new ol.layer.Tile({
		source: new ol.source.XYZ({
			url: "https://cyberjapandata.gsi.go.jp/xyz/english/{z}/{x}/{y}.png",
			projection: "EPSG:3857"
		})
	});

	const vectorSource = new ol.source.Vector({
		features: trackLineArray,
	}); // source for vector layer 

	// vector layer for ground track of satellites
	const lineVector = new ol.layer.Vector({
		source: vectorSource,
		style: new ol.style.Style({
			stroke: new ol.style.Stroke({ color: 'blue', width: 2 })
		})
	});

	let map = new ol.Map({
		//		layers: [osmLayer, rabelLayer, lineVector],
		layers: [gsiLayer, rabelLayer, lineVector],
		target: document.getElementById('map'),
		view: new ol.View({
			center: convertCoordinate(131.129172, 12.068235),
			zoom: 2
		}),
		controls: ol.control.defaults()
	});

	/**
	 * popup window for a satellite
	 */
	let element = document.getElementById('popup');

	let popup = new ol.Overlay({
		element: element,
		positioning: 'bottom-center',
		stopEvent: false,
		offset: [10, 0]
	});
	map.addOverlay(popup);

	//alert("#4 here!!");

	/*
	 * Event when click
	 */
	map.on('click', function (_event) {
		if (!isDrawn) {
			startPlot(paramms[1]);
			isDrawn = true;
		}
	});

	/*
	 * Event when double click
	 */
	/*
	google.maps.event.addListener(map, 'dblclick', function(event) {
		clearTimeout(update_timeout);
		// alert("here doubleclick event");
		trackCoordinatesArray.forEach(function(ele, index, array) {
			trackLineArray[index].setMap(null);
			markerArray[index].setMap(null);
			trackCoordinatesArray[index] = new Array();
		});
		for (var i = 0; i < satArray.length; i++) {
			satNo[i] = 0;
		}
		isDrawn = false;
	});
*/

	/**
	 * when satellite is clicked
	 */
	let select = new ol.interaction.Select();
	
	map.addInteraction(select);
	select.on('select', function (e) {
		//		let c =e.target.getFeatures().getLength(); 
		if (e.target.getFeatures().getLength() > 0) {
			let obj = e.target.getFeatures().item(0);
			let name = obj.get('name');
			if (name) {//if name is not undefined
				let id = e.target.getFeatures().item(0).get('id');
				let date = obj.get('date');
				let time = obj.get('time');
				element.innerHTML = '<code>NORAD ID:' + id + '</code><br/>' + name+
				'<br><code>'+date + ' '+time+'</code>';
				let coordinates = e.target.getFeatures().item(0).getGeometry().getCoordinates();
				popup.setPosition(coordinates);
			}
		} else {
		}
	});

	$("#loading").fadeOut(500);
}

/**
 * input data to trackCoordinatesArray (2 dimention array) from values gotten via tlews with jsonp format.
 * @param e
 *            element of values
 */
function createTrackCoordinateArray(e) {
	satArray.some(function (ele_sat, i) {
		if (e.SatObservation.SatelliteNumber == ele_sat.catNo) {
			//if (e.SatObservation.Sensor.SensorLocation.Longitude < 0) {
				trackCoordinatesArray[i][satNo[i]] = [
					e.SatObservation.Sensor.SensorLocation.Longitude,
					e.SatObservation.Sensor.SensorLocation.Latitude,
					e.SatObservation.ObDate,
					e.SatObservation.ObTime
				];
/*
			} else {
				trackCoordinatesArray[i][satNo[i]] = [
					e.SatObservation.Sensor.SensorLocation.Longitude,
					e.SatObservation.Sensor.SensorLocation.Latitude];
					e.SatObservation.ObDate,
					e.SatObservation.ObTime
			}*/
			satNo[i]++;

			return;
		}
	});
}

//google.maps.event.addDomListener(window, 'load', initialize);
