function rrand(min, max) {
    return Math.random() * (max - min) + min;
}

Array.prototype.choose = function () {
    return this[Math.floor(Math.random() * this.length)]
}