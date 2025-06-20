const boardElement = document.getElementById('board');
const statusElement = document.getElementById('status');

let gameId = "";

let selectedCell = null;
let boardState = {};
let selectedPieceType = "";
let from = "";

let currentGameId = null;
let myTurn = false;

function createBoard() {
    console.log("отрисовываем доску");
    for (let row = 0; row < 8; row++) {
        for (let col = 0; col < 8; col++) {
            const cell = document.createElement('div');
            cell.classList.add('cell');
            if ((row + col) % 2 === 0) {
                cell.classList.add('white');
            } else {
                cell.classList.add('black');
            }

            cell.dataset.row = row;
            cell.dataset.col = col;

            cell.addEventListener('click', () => handleCellClick(cell));

            boardElement.appendChild(cell);
        }
    }
}

let pollInterval = null;

async function pollGameStatus() {
    pollInterval = setInterval(async () => {
        if (!gameId) {
            clearInterval(pollInterval);
            return;
        }

        const response = await fetch(`http://localhost:8080/api/game/status/${gameId}`, {
            headers: { "Authorization": `Bearer ${localStorage.getItem("token")}` }
        });

        const data = await response.json();

        if (data.turn !== myTurn) {
            updateBoard(data.board);
            myTurn = true;
            clearInterval(pollInterval);
        }
    }, 100); // Проверяем каждые 2 секунды
}

function updateBoard(newBoardState) {
    boardState = newBoardState; // Сохраняем последнее состояние доски

    const cells = document.querySelectorAll('.cell');
    cells.forEach(cell => {
        cell.innerHTML = '';
        cell.classList.remove('selected');
    });

    for (let key in newBoardState) {
        const [colStr, rowStr] = key.split('');
        const col = parseInt(colStr);
        const row = parseInt(rowStr);
        const displayedRow = 7 - row;

        const cell = document.querySelector(`[data-row="${displayedRow}"][data-col="${col}"]`);
        if (!cell) continue;

        const piece = newBoardState[key];
        const img = document.createElement('div');
        img.className = 'piece';
        img.style.backgroundImage = getPieceImage(piece.type, piece.color);
        cell.appendChild(img);
    }
}

async function joinGame(gameIdt) {
    const token = localStorage.getItem("token");
    if (!token) {
        alert("Сначала войдите в систему");
        return false;
    }

    try {
        const response = await fetch(`http://localhost:8080/api/game/join/${gameIdt}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        const data = await response.json();

        if (response.ok) {
            alert("Вы успешно присоединились к игре");
            gameId = gameIdt;
            showPage('game');
            fetchGameStatus();
        } else {
            alert(data.message || "Не удалось присоединиться к игре");
        }
    } catch (err) {
        console.error("Ошибка присоединения к игре:", err);
        alert("Произошла ошибка при присоединении к игре");
    }
}

function getPieceImage(type, color) {
    return 'url(https://upload.wikimedia.org/wikipedia/commons/thumb/' + getImagePath(type,  color) + ')';
}

function getImagePath(type, color) {
    switch (type) {
        case "rook":
            return color === "white" ? "7/72/Chess_rlt45.svg/1280px-Chess_rlt45.svg.png" : "f/ff/Chess_rdt45.svg/1280px-Chess_rdt45.svg.png";
        case "knight":
            return color === "white" ? "7/70/Chess_nlt45.svg/1280px-Chess_nlt45.svg.png" : "e/ef/Chess_ndt45.svg/1280px-Chess_ndt45.svg.png";
        case "bishop":
            return color === "white" ? "b/b1/Chess_blt45.svg/1280px-Chess_blt45.svg.png" : "9/98/Chess_bdt45.svg/1280px-Chess_bdt45.svg.png";
        case "queen":
            return color === "white" ? "1/15/Chess_qlt45.svg/1280px-Chess_qlt45.svg.png" : "4/47/Chess_qdt45.svg/1280px-Chess_qdt45.svg.png";
        case "king":
            return color === "white" ? "4/42/Chess_klt45.svg/1280px-Chess_klt45.svg.png" : "f/f0/Chess_kdt45.svg/1280px-Chess_kdt45.svg.png";
        case "pawn":
            return color === "white" ? "4/45/Chess_plt45.svg/1280px-Chess_plt45.svg.png" : "c/c7/Chess_pdt45.svg/1280px-Chess_pdt45.svg.png";
        default:
            return "";
    }
}

function removeCss(...classes) {
    document.querySelectorAll('.cell').forEach(cell => {
        classes.forEach(cls => cell.classList.remove(cls));
    });
}

function handleCellClick(cell) {

    const row = parseInt(cell.dataset.row);
    const col = parseInt(cell.dataset.col);

    console.log(row + " " + col + " " + from);
    removeCss('possible','selected');
    if (selectedCell === null) {

        from = String.fromCharCode(97 + col) + (8 - row); // a2, b3 и т.д.
        selectedCell = cell;

        fetchPossibleMoves(from);

        cell.classList.add('selected');
    } else {
        if (parseInt(selectedCell.dataset.row) === row && parseInt(selectedCell.dataset.col) === col) {
            selectedCell = null;
            from = "";
        } else {
            let to = String.fromCharCode(97 + col) + (8 - row);
            if(makeMove(from, to, boardState[chessToIndex(from)]?.type || "")){
                selectedCell.classList.remove('selected');
                selectedCell = null;
                selectedPieceType = "";
            }else{
                from = String.fromCharCode(97 + col) + (8 - row); // a2, b3 и т.д.
                selectedCell = cell;

                fetchPossibleMoves(from);

                cell.classList.add('selected');
            }
        }
    }
}

async function createNewGame() {
    try {
        const token = localStorage.getItem('token');

        console.log("токен на создание игры = " + localStorage.getItem('token'));
        const response = await fetch('http://localhost:8080/api/game/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`

            }
        });

        const data = await response.json();
        console.log("Ответ от сервера:", data);

        if (data.gameId) {
            gameId = data.gameId;
            showPage('game');
            fetchGameStatus(); // Загрузка состояния новой игры
        } else {
            alert("Не удалось создать игру");
        }
    } catch (err) {
        console.error("Ошибка создания игры:", err);
        alert("Не удалось создать игру");
    }
}


// auth.js или script.js
function fetchAllGames() {
    const token = localStorage.getItem("token");
    if (!token) {
        alert("Сначала войдите в систему");
        return;
    }

    fetch('http://localhost:8080/api/games', {
        headers: {
            "Authorization": `Bearer ${token}`
        }
    })
        .then(res => {
            if (!res.ok) {
                throw new Error("HTTP error: " + res.status);
            }
            return res.json();
        })
        .then(games => {
            const list = document.getElementById('game-list');
            list.innerHTML = ''; // Очистка списка перед загрузкой

            games.forEach(game => {
                const li = document.createElement('li');
                li.textContent = game.id + ' - ' + game.status;

                // Проверяем, есть ли уже два игрока
                const isFull = game.whitePlayer && game.blackPlayer;

                if (isFull) {
                    li.textContent += ' (Игра заполнена)';
                    li.style.color = 'gray';
                } else {
                    // Если место есть — можно присоединиться
                    li.addEventListener('click', () => {
                        console.log("Тестовое присоединение по id = " + game.id);
                        joinGame(game.id);
                    });
                }

                list.appendChild(li);
            });
        })
        .catch(err => {
            console.error("Ошибка получения партий:", err);
            alert("Не удалось получить список партий");
        });
}

function fetchPossibleMoves(fromStr) {

    removeCss('possible', 'selected');

    fetch(`http://localhost:8080/api/game/moves/${gameId}/${fromStr}`, {
        headers: {
            "Authorization": `Bearer ${localStorage.getItem("token")}`
        }
    }).then(res => res.json())
        .then(moves => {
            console.log("Возможные ходы:", moves);


            for (let move of moves[fromStr] || []) {
                const col = move.charCodeAt(0) - 'a'.charCodeAt(0);
                const displayedRow = 8 - parseInt(move.charAt(1));
                const targetCell = document.querySelector(`[data-row="${displayedRow}"][data-col="${col}"]`);
                if (targetCell) {
                    targetCell.classList.add('possible');
                }
            }
        })
        .catch(err => {
            console.error("Ошибка получения ходов:", err);
            alert("Не удалось получить возможные ходы");
        });
}


function makeMove(from, to, pieceType) {
    if (!gameId) {
        alert("Сначала создайте игру");
        return false;
    }

    fetch('http://localhost:8080/api/game/move', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json',
            "Authorization": `Bearer ${localStorage.getItem("token")}`},
        body: JSON.stringify({
            gameId: gameId,
            move: {
                from: from,
                to: to,
                piece: pieceType
            }
        })
    })
        .then(() => {
            myTurn = false;
            pollGameStatus(); // Начинаем ждать ход соперника
        })
        .catch(err => {
            console.error("Ошибка:", err);
            return false;
        });
}

function chessToIndex(posStr) {
    const col = posStr.charCodeAt(0) - 'a'.charCodeAt(0);
    const row = 8 - parseInt(posStr.charAt(1)); // Инвертируем строку для серверных координат
    return `${col}${row}`;
}


let isFetchingGameStatus = false;

async function fetchGameStatus() {
    if (isFetchingGameStatus) return; // Игнорируем, если уже идёт загрузка
    isFetchingGameStatus = true;

    console.log("Получаю статус игры... по id = " + gameId);

    fetch(`http://localhost:8080/api/game/status/${gameId}`, {
        headers: {
            "Authorization": `Bearer ${localStorage.getItem("token")}`
        }
    }).then(res => {
        if (!res.ok) {
            return res.text().then(text => { throw new Error(text); });
        }
        return res.json();
    })
        .then(data => {
            if (document.getElementById('game-page').style.display === 'block') {
                console.log("Данные получены:", data);
                statusElement.textContent = `Ход: ${data.turn}, Статус: ${data.status}`;
            }
            updateBoard(data.board);
            statusElement.textContent = `Ход: ${data.turn}, Статус: ${data.status}`;
        })
        .catch(err => {
            console.error("Ошибка получения статуса:", err.message);
            statusElement.textContent = "Не удалось получить статус игры";
        })
        .finally(() => {
            isFetchingGameStatus = false;
        });

}


// логика страниц

let boardCreated = false;

function showPage(pageId) {
    document.querySelectorAll('.page').forEach(page => {
        page.style.display = 'none';
    });
    const selectedPage = document.getElementById(pageId + "-page");
    if (selectedPage) {
        selectedPage.style.display = 'block';

        if (pageId === 'game' && !boardCreated) {
            createBoard();
            boardCreated = true;
        }

        if (pageId === 'game') {
            fetchGameStatus(); // Только если есть gameId
        }

        if (pageId === 'games') {
            fetchAllGames(); // Загружаем список партий
        }
    }
}

function checkAuthStatus() {
    const token = localStorage.getItem("token");
    if (token) {
        // Проверка валидности токена
        validateToken(token).then(isValid => {
            if (isValid) {
                showPage('game'); // Перенаправляем на игровую страницу
            } else {
                logout(); // Или на страницу входа
            }
        });
    }
}

// Функция для проверки валидности токена
async function validateToken(token) {
    try {
        const response = await fetch('http://localhost:8080/api/auth/validate', {
            method: 'GET',
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!response.ok) {
            throw new Error('Invalid token');
        }

        return true;
    } catch (error) {
        console.error('Token validation error:', error);
        return false;
    }
}

function showGamesList() {
    fetch('http://localhost:8080/api/games')
        .then(res => res.json())
        .then(games => {
            const list = document.getElementById('game-list');
            list.innerHTML = '';
            games.forEach(game => {
                const li = document.createElement('li');
                li.textContent = game.id + ' - ' + game.status;

                li.addEventListener('click', () => {
                    gameId = game.id;
                    showPage('game');
                    fetchGameStatus();
                });

                list.appendChild(li);
            });
        })
        .catch(err => {
            console.error("Ошибка получения партий:", err);
            alert("Не удалось получить список партий");
        });
}

// По умолчанию показываем главную страницу
window.addEventListener("load", () => {
    showPage('home');
});




