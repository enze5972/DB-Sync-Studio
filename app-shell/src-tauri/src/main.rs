#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod backend;

use backend::{append_startup_log_line, start_backend, BackendHandle};
use tauri::{Manager, State};

#[tauri::command]
fn get_backend_base_url(state: State<BackendHandle>) -> String {
    state.base_url().to_string()
}

fn main() {
    append_startup_log_line("Starting DB Sync Studio desktop shell");
    tauri::Builder::default()
        .setup(|app| {
            let backend_handle = match start_backend(app.handle()) {
                Ok(handle) => handle,
                Err(err) => {
                    append_startup_log_line(&format!("Shell startup failed: {}", err));
                    return Err(err.into());
                }
            };
            app.manage(backend_handle);
            append_startup_log_line("Desktop shell started successfully");
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![get_backend_base_url])
        .run(tauri::generate_context!())
        .expect("error while running DB Sync Studio");
}
