function startListen() {
    var grid = document.getElementsByClassName("grid-container");
    if (grid.length > 0) {
        var observer = new MutationObserver(changeCallback);
        var options = {
            'childList': true
        };
        observer.observe(grid[0], options);
    } else {
        console.log("No Grid Element Found")
    }
}

function changeCallback(mutations, observer) {
    mutations.forEach(function(mutation) {
        console.log(mutation)
    })
}

startListen()