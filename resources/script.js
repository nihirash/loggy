"use strict";

/**
 * Infinite scroll goes here
 **/

var pageLoaded = 0;
var toLoadPage = true;
var locked = false;

function loadPage()
{
    if (!toLoadPage) {
	return ;
    }
    
    locked = true;
    var pageToLoad = ++ pageLoaded;
    fetch("/page/" + pageToLoad)
	.then(function (response) {
	    return response.text();
	}).then(function(data) {
	    if (data == "") {
		toLoadPage = false;
		return ;
	    }
	    
	    document.getElementsByClassName('siteMain')[0]
		.insertAdjacentHTML('beforeend', data);
	    locked = false;
	});
}
/**
 * Infinite scroll
 **/
function scrollDown(e)
{   
    var cont            = document.body,
	fullHeight     = cont.scrollHeight,
	viewportHeight = window.innerHeight,
	scrolled        = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop,
	scrolledBottom = scrolled + viewportHeight,
	atBottom       = scrolledBottom >= fullHeight - 200;
    
    if (atBottom && !locked) {
	loadPage();
    }
}

if (window.location.pathname === '/') {
    window.addEventListener("load", function () {
	scrollDown();
	window.addEventListener("scroll", scrollDown);
    });
}
