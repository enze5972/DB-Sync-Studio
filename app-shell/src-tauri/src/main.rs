#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod backend;

use backend::{start_backend, BackendHandle};
use tauri::{Manager, State};

#[tauri::command]
fn get_backend_base_url(state: State<BackendHandle>) -> String {
    state.base_url().to_string()
}

fn main() {
    tauri::Builder::default()
        .setup(|app| {
            let backend_handle = start_backend(app.handle())?;
            app.manage(backend_handle);
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![get_backend_base_url])
        .run(tauri::generate_context!())
        .expect("error while running DB Sync Studio");
}
