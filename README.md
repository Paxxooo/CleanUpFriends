# Clean up Friends for Habbo 🧹👥

## Italiano 🇮🇹

### Descrizione
Questa estensione permette di rimuovere tutti gli amici inattivi da Habbo prima di una specifica data. Utile per mantenere la lista amici pulita e organizzata.

### Come si usa
1. **Configurazione**: Prima di tutto, è necessario configurare il file `config.ini`. Inserisci il tuo `username` di Habbo e la `BeforeDate` (data prima della quale cercherai gli amici inattivi nel formato `YYYY-MM-DD`).

    Esempio di `config.ini`:
    ```
    username=il_tuo_username
    BeforeDate=2024-01-01
    ```

2. **Generazione della lista**: Scrivi `:generatelist` nel client di Habbo. Verrà generata la lista degli amici inattivi prima della data inserita e salvata in `usernames.json`.

3. **Rimozione degli amici**: Dopo aver generato la lista, scrivi `:removefriends` per iniziare il processo di rimozione degli amici elencati nel file `usernames.json`.

### Note aggiuntive
- Il programma genera anche un file `profileprivate.json` che contiene tutti gli amici che hanno un profilo invisibile. Se si desidera rimuovere anche questi, copia e incolla il loro contenuto nel file `usernames.json` prima di eseguire `:removefriends`.

## English 🇬🇧

### Description
This extension allows you to remove all inactive friends on Habbo before a specific date. Useful for keeping your friends list clean and organized.

### How to Use
1. **Setup**: First, you need to set up the `config.ini` file. Enter your Habbo `username` and `BeforeDate` (the date before which you will search for inactive friends in the `YYYY-MM-DD` format).

    `config.ini` example:
    ```
    username=your_username
    BeforeDate=2024-01-01
    ```

2. **Generating the List**: Type `:generatelist` in the Habbo client. This will generate a list of friends inactive before the entered date and save it in `usernames.json`.

3. **Removing Friends**: After generating the list, type `:removefriends` to start the process of removing the friends listed in `usernames.json`.

### Additional Notes
- The program also generates a `profileprivate.json` file containing all friends with an invisible profile. If you wish to remove these as well, copy and paste their content into the `usernames.json` file before running `:removefriends`.
