
/**
 * use this function when you want to cross date line.
 * this function can divide linestring on the point on 180 degree.
 */
let groundTrackFeatures = function (ele) {
    let result = new Array();
    let lat_on_180 = 0;
    let last_lat_on180 = 0;

    /**
     * direction:
     * 1 -> east to west (ex. -179 -> 179)
     * -1 -> west to east (ex. 179 -> -179)
     */
    let direction = -9999;

    let cursor = 0;
    //v: value, i: index, a: array
    ele.forEach(function (v, i, a) {
        if (i > 0) {
            //from second value of the array, check the line cross or not
            if (v[0] - a[i - 1][0] < -180) {
                //case for crossing from west to east
                direction = -1;
                //v[0] less than 0 so should be +360
                cursor = devideArray(cursor, direction, result, v, i, a);

            } else if (v[0] - a[i - 1][0] > 180) {
                //case for crossing from east to west
                direction = 1;

                cursor = devideArray(cursor, direction, result, v, i, a);
            } else {
                // case for no crossing, do nothing
                //      alert("hey #8:(" + v[0] + ", " + v[1] + ")");

            }
        } if (i == ele.length - 1) {
            // last value of this array
            // alert("hey #9: " + ele.length);
            let tempArray2 = new Array();

            pushArray(cursor, result, tempArray2,ele.length,ele);

            result.push(tempArray2);

        }
    });

    return result;
}

/**
 * get latitude on 180 longitude,
 * create new array from cursor to cross point by using a.
 * created array will be added result array.
 * @param {*} cursor 
 * @param {*} direction 
 * @param {*} result 
 * @param {*} v 
 * @param {*} i 
 * @param {*} a 
 */
let devideArray = function (cursor, direction, result, v, i, a) {
    let tempArray = [];
    const delta_lon = v[0] - direction * 360 - a[i - 1][0];
    const delta_lat = v[1] - a[i - 1][1];
    lat_on_180 = v[1] - (- direction * 180 + v[0]) * delta_lat / delta_lon;

    pushArray(cursor, result, tempArray,i,a);
    tempArray.push([-1 * 180 * direction, lat_on_180])
    result.push(tempArray);
    return i;
}

/**
 * 
 * @param {*} cursor 
 * @param {*} result 
 * @param {*} tempArray 
 * @param {*} i 
 * @param {*} a 
 */

let pushArray = function(cursor, result, tempArray, i, a){
    
    if (cursor > 0) {
        let lastArray = result[result.length - 1];
        let lastCoord = lastArray[lastArray.length - 1];
        tempArray.push([
            lastCoord[0] * -1,
            lastCoord[1]
        ]);//lastCoord[0] should 180 or -180
    }
    for (let j = cursor; j < i; j++) {
        tempArray.push(a[j]);
    }
    
}