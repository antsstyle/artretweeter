function compare(a, b) {
    if (!Number.isNaN(Date.parse(a)) && a.includes("-")) {
        return Date.parse(a) > Date.parse(b);
    } else if (!Number.isNaN(parseInt(a))) {
        return parseInt(a) > parseInt(b);
    }
    return a.toLowerCase() > b.toLowerCase();
}

function swapRows(row1, row2) {
    let count = row1.length;
    for (let i = 0; i < count; i++) {
        let temp = row1[i].innerHTML;
        row1[i].innerHTML = row2[i].innerHTML;
        row2[i].innerHTML = temp;
    }
}

function checkTableSorted(n, tableID) {
    let table = document.getElementById(tableID);
    let rows = table.rows;
    let sortedAsc = true;
    for (let i = 1; i < rows.length - 1; i++) {
        let x = rows[i].getElementsByTagName("TD")[n];
        let y = rows[i + 1].getElementsByTagName("TD")[n];
        if (compare(x.innerHTML, y.innerHTML)) {
            sortedAsc = false;
            break;
        }
    }
    return sortedAsc;
}

function sortTable(n, tableID) {
    let table = document.getElementById(tableID);
    if (checkTableSorted(n, tableID)) {
        quickSort(table.rows, 1, table.rows.length - 1, n, true);
    } else {
        quickSort(table.rows, 1, table.rows.length - 1, n, false);
    }

}

function partition(rows, start, end, n, descorder) {
    const pivotValue = rows[end].getElementsByTagName("TD")[n].innerHTML;
    let pivotIndex = start;
    for (let i = start; i < end; i++) {
        let rowValue = rows[i].getElementsByTagName("TD")[n].innerHTML;
        if (descorder) {
            if (compare(rowValue, pivotValue)) {
                swapRows(rows[i].getElementsByTagName("TD"), rows[pivotIndex].getElementsByTagName("TD"));
                pivotIndex++;
            }
        } else {
            if (compare(pivotValue, rowValue)) {
                swapRows(rows[i].getElementsByTagName("TD"), rows[pivotIndex].getElementsByTagName("TD"));
                pivotIndex++;
            }
        }

    }

    swapRows(rows[pivotIndex].getElementsByTagName("TD"), rows[end].getElementsByTagName("TD"));
    return pivotIndex;
}

function quickSort(arr, start, end, n, descorder) {
    if (start >= end) {
        return;
    }
    let index = partition(arr, start, end, n, descorder);

    if (start < index - 1) {
        quickSort(arr, start, index - 1, n, descorder);
    }
    if (index < end) {
        quickSort(arr, index + 1, end, n, descorder);
    }

}

// Assumes table is in ascending order
function findRowAndColumnForTwitterHandle(tableID, comparisonCell, maxColumns) {
    let table = document.getElementById(tableID);
    let rows = table.rows;
    for (var i = 0; i < rows.length; i++) {
        let columnCount = rows[i].getElementsByTagName("TD").length;
        for (var j = 0; j < columnCount; j++) {
            let cellHTML = rows[i].getElementsByTagName("TD")[j].innerHTML;
            if (comparisonCell.toLowerCase() <= cellHTML.toLowerCase()) {
                return [i, j];
            }
        }
    }
    if (j === maxColumns) {
        return [i, 0];
    } else {
        return [i - 1, j];
    }

}

function insertIntoTable(newRowIndex, newColumnIndex, newElement, tableID, maxColumns) {
    let table = document.getElementById(tableID);
    let rows = table.rows;
    let rowsLength = rows.length;
    let oldRow = rows[newRowIndex];
    if (newRowIndex === rows.length - 1 && rows[rowsLength - 1].getElementsByTagName("TD").length < maxColumns
            && newColumnIndex >= rows[rowsLength - 1].getElementsByTagName("TD").length) {
        let cell = oldRow.insertCell();
        cell.innerHTML = newElement;
        return;
    } else if (newRowIndex >= rows.length) {
        let row = table.insertRow();
        let cell = row.insertCell();
        cell.innerHTML = newElement;
        return;
    }
    var nextValue = oldRow.getElementsByTagName("TD")[newColumnIndex].innerHTML;
    for (let i = newColumnIndex; i < oldRow.getElementsByTagName("TD").length - 1; i++) {
        let temp = oldRow.getElementsByTagName("TD")[i + 1].innerHTML;
        oldRow.getElementsByTagName("TD")[i + 1].innerHTML = nextValue;
        nextValue = temp;
    }
    oldRow.getElementsByTagName("TD")[newColumnIndex].innerHTML = newElement;
    for (let i = newRowIndex + 1; i < rowsLength; i++) {
        let currentRow = rows[i];
        let currentRowLength = currentRow.getElementsByTagName("TD").length;
        for (let j = 0; j < currentRowLength; j++) {
            let temp = currentRow.getElementsByTagName("TD")[j].innerHTML;
            currentRow.getElementsByTagName("TD")[j].innerHTML = nextValue;
            nextValue = temp;
        }
    }
    let lastRow = rows[rowsLength - 1];
    if (lastRow.getElementsByTagName("TD").length === maxColumns) {
        let row = table.insertRow();
        let cell = row.insertCell();
        cell.innerHTML = nextValue;
    } else {
        let cell = lastRow.insertCell();
        cell.innerHTML = nextValue;
    }
}