var observer

function startListen() {
    Log.d("Start Listen in JavaScript")
    var grid = document.getElementsByClassName("grid-container");
    if (grid.length > 0) {
        observer = new MutationObserver(changeCallback);
        var options = {
            'childList': true
        };
        observer.observe(grid[0], options);
        Log.w("added observer")
    } else {
        Log.e("No Grid Element Found, can't find element which that's name is 'grid-container'")
    }
}

function changeCallback(mutations, observer) {
    mutations.forEach(function (mutation) {
        var addedNodes = mutation.addedNodes;
        var length = addedNodes.length;
        if (length > 0) {
            addedNodes.forEach(function (node) {
                if (node.className.startsWith("photo_thumbnail")) {
                    var href = node.getElementsByClassName("photo_link")[0].getAttribute("href");
                    JsCallback.addPhotoDetail(href)
                }
            })
        }
    })
}

startListen()
