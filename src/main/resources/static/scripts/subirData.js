function updateFileName() {
    var fileInput = document.getElementById('fileInput');
    var fileNameDisplay = document.getElementById('fileNameDisplay');

    if (fileInput.files.length > 0) {
        var fileName = fileInput.files[0].name;
        fileNameDisplay.textContent = 'Archivo seleccionado: ' + fileName;
        fileNameDisplay.style.color = "black"; 
    } else {
        fileNameDisplay.textContent = 'No se ha seleccionado ningún archivo';
        fileNameDisplay.style.color = "red";
    }
}

