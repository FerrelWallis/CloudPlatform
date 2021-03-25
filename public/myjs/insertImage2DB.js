

function base64(file) {
    var reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = function () {
       console.log(this.result);
    };
};