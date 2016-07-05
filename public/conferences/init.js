/*
    Generic functions & widgets
 */

// http://fullcalendar.io/
(function(){
    var fullCalendarDateFormat = 'YYYY-MM-DD';
    var apiDateFormat = 'DD/MM/YYYY';
    var apiResults = {};
    $(document).ready(function() {
        $('.full-calendar').each(function(){
            var $elt = $(this);
            $elt.fullCalendar({
                lang: $elt.attr('lang') || 'en',
                header: {
                    left: '',
                    center: 'title',
                    right: 'today prev,next'
                },
                defaultDate: tryInt($elt.attr('default-date')) || null,
                events: getEvents($elt) || [],
                height: tryInt($elt.attr('height')),
                editable: false,
                eventLimit: true
            });
        });
    });
    function getEvents($elt) {
        // static list of events
        var eventsJson = $elt.attr('events');
        if(eventsJson){ return JSON.parse(eventsJson); }
        // events fetched from API
        var remoteEventsJson = $elt.attr('remote-events');
        if(remoteEventsJson){
            var config = JSON.parse(remoteEventsJson);
            return function(start, end, timezone, callback){
                var url = config.searchUrl.replace('START_DATE', start.format(apiDateFormat)).replace('END_DATE', end.format(apiDateFormat));
                if(apiResults[url]){
                    callback(apiResults[url]);
                } else {
                    $.ajax({
                        url: config.searchUrl.replace('START_DATE', start.format(apiDateFormat)).replace('END_DATE', end.format(apiDateFormat)),
                        success: function(data) {
                            apiResults[url] = data.result.map(function(e){
                                return {
                                    title: e.name,
                                    start: moment(e.start).format(fullCalendarDateFormat),
                                    end: moment(e.end).add(1, 'days').format(fullCalendarDateFormat), // fullCalendar exclude last day
                                    url: config.eventUrl.replace('ID', e.id)
                                };
                            });
                            callback(apiResults[url]);
                        }
                    });
                }
            };
        }
    }
    function tryInt(str) {
        try {
            return parseInt(str);
        } catch (e) {
            return str;
        }
    }
})();

// map-view
function initMap(){
    var paris = {lat: 48.85661400000001, lng: 2.3522219000000177};
    $('.gmap-view').each(function() {
        var $map = $(this);
        var gMap = new google.maps.Map($map.get(0), {center: paris, zoom: 5});
        var gInfowindow = new google.maps.InfoWindow();
        var markers = jsonAttr($map, 'markers') || [];
        var gMarkers = markers.map(function(marker){
            var gMarker = new google.maps.Marker({
                map: gMap,
                position: {lat: marker.lat, lng: marker.lng},
                title: marker.title
            });
            gMarker.addListener('click', function(){
                showInfo(gMap, gMarker, gInfowindow, marker);
            });
            return gMarker;
        });
        fitBounds(gMap, gMarkers);
    });
    function fitBounds(gMap, gMarkers){
        var gBounds = new google.maps.LatLngBounds();
        gMarkers.map(function(gMarker){
            gBounds.extend(gMarker.getPosition());
        });
        gMap.fitBounds(gBounds);
    }
    function showInfo(gMap, gMarker, gInfowindow, marker){
        gInfowindow.setContent(
            '<strong>'+marker.title+'</strong><br>'+
            marker.location+'<br>'+
            'le '+marker.date
        );
        gInfowindow.open(gMap, gMarker);
    }
    function jsonAttr($elt, name){
        var json = $elt.attr(name);
        if(json){
            try {
                return JSON.parse(json);
            } catch (e){

            }
        }
    }
}

// called by google maps
function googleMapsInit(){
    initPlacePicker();
    initMap();
}
