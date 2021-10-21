// Плагин для автоматического входа на сервер
// написан специально для сервера Novice проекта WarMine, под другие сервера нужно вносить правки

/*
// срабатывает при запуске программы
// ocr - объект класса OCR.java
function initialize(ocr) {

}
*/

/*
// срабатывает при перезагрузке плагина
// ocr - объект класса OCR.java
function reload(ocr) {

}
*/

/*
// срабатывает в потоке макросов, если макрос неактивен, задержка 5 мс,
// в противном случае - в зависимости от задержки макроса
function tick() {

}
*/

// срабатывает после завершения обработки скриншота экрана
// gameLineList - объект класса ArrayList<String>, содержит в себе список найденных слов на экране
function run(gameLineList) {
    var onlineGameButton = null, returnToGameButton = null, refreshConnectButton = null, serverConnectionButton = null, serverButton = null;
    for(var i = 0; i < gameLineList.size(); ++i) {
        var gameLine = gameLineList.get(i);
        if(gameLine.line.equals("Сетевая игра") || gameLine.line.equals("Начать играть")) {
            onlineGameButton = gameLine;
        } else if(gameLine.line.equals("Вернуться в главное меню")) {
            returnToGameButton = gameLine;
        } else if(gameLine.line.equals("Обновить")) {
            refreshConnectButton = gameLine;
        } else if(gameLine.line.equals("Подключиться")) {
            serverConnectionButton = gameLine;
        }

        if(gameLine.line.equals("Хелоуин, 1.6.6!") || gameLine.line.equals("Хелоуин,") || gameLine.line.equals("A Мinесrаft Sеrvеr")) {
            serverButton = gameLine;
        }
    }

    if(onlineGameButton != null || returnToGameButton != null) {
        pressMouseLeft(onlineGameButton != null ? onlineGameButton : returnToGameButton, 0);
    } else if(refreshConnectButton != null) {
        if(serverButton == null) {
            // жмем обновить, пока не сможем подключиться к серверу
            pressMouseLeft(refreshConnectButton, 200);
            java.lang.Thread.sleep(5000);
        } else {
            var tempActiveAction = com.github.dreamsmoke.ocr.util.Settings.ACTION_ENABLE;
            if(tempActiveAction) {
                java.lang.System.out.println("Временно отключаем макрос.");
                com.github.dreamsmoke.ocr.util.Settings.ACTION_ENABLE = false;
            }

            // нажимаем несколько раз на сервер, пытаемся зайти, или хотя бы выбрать активным
            pressMouseLeft(serverButton, 50);
            java.lang.Thread.sleep(200);
            pressMouseLeft(serverButton, 50);
            java.lang.Thread.sleep(500);

            // если есть возможность нажать кнопку 'Подключиться', делаем это
            if(serverConnectionButton != null) {
                pressMouseLeft(serverConnectionButton, 200);
            }

            java.lang.System.out.printf("Подключаемся к серверу (%s)...%n", serverButton.line);
            java.lang.Thread.sleep(2500);

            if(tempActiveAction) {
                java.lang.System.out.println("Возвращаем работу макроса.");
                com.github.dreamsmoke.ocr.util.Settings.ACTION_ENABLE = true;
            }
        }
    }
}

/*
// срабатывает при сохранении конфигурации
// ocr - объект класса OCR.java
// configuration - объект класса OCRConfiguration.java
function save(ocr, configuration) {

}
*/

/*
// срабатывает при выключении программы
function exit() {

}
*/

// нажимаем на кнопку в окне игры по найденному имени
function pressMouseLeft(gameLine, delay) {
	java.lang.System.out.printf("Press %s button, x: %s, y: %s, w: %s, h: %s.%n", gameLine.line, gameLine.posX, gameLine.posY, gameLine.width, gameLine.height);

    // перемещаемся в середину кнопки
	robot.mouseMove(gameLine.posX + gameLine.width / 2, gameLine.posY + gameLine.height / 2);
	robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);

	// ждем немного времени перед отжатием левой кнопки мыши
	java.lang.Thread.sleep(delay);

    robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
}