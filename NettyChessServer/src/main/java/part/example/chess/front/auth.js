// auth.js - реализация регистрации и входа

// Регистрация пользователя
async function registerUser(event) {
    event.preventDefault(); // Предотвращаем перезагрузку страницы

    const username = document.getElementById('username').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value.trim();

    if (!username || !email || !password) {
        alert('Заполните все поля');
        return;
    }

    try {
        const response = await fetch('http://localhost:8081/api/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username,
                email,
                password
            })
        });

        const data = await response.json();

        if (response.ok) {
            alert('Регистрация успешна! Теперь вы можете войти.');
            showPage('login'); // Переключаемся на страницу входа
        } else {
            alert(data.message || 'Ошибка регистрации');
        }
    } catch (error) {
        console.error('Registration error:', error);
        alert('Произошла ошибка при регистрации');
    }
}

// Вход в систему
async function loginUser() {
    event.preventDefault();
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value.trim();

    if (!username || !password) {
        alert('Заполните все поля');
        return;
    }

    try {
        const response = await fetch('http://localhost:8081/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username,
                password
            })
        });

        const data = await response.json();

        if (response.ok) {
            console.log('token от сервиса', data.token);
            localStorage.setItem('token', data.token);
            alert('Вы успешно вошли');

            // Получаем информацию о пользователе
            const userResponse = await fetch('http://localhost:8081/api/auth/user-info', {
                headers: {
                    "Authorization": `Bearer ${data.token}`
                }
            });

            if (userResponse.ok) {
                const userData = await userResponse.json();
                currentUser = { ...userData };

                // Перенаправляем на игровую страницу
                showPage('game');
            }
        } else {
            alert(data.message || 'Неверный логин или пароль');
        }
    } catch (error) {
        console.error('Login error:', error);
        alert('Произошла ошибка при входе');
    }
}

// Выход из системы
function logout() {
    localStorage.removeItem('token');
    currentUser = null;
    alert('Вы вышли из системы');
    showPage('home');
}