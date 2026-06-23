use std::env;
use std::fs::{create_dir_all, File, OpenOptions};
use std::io;
use std::io::Write;
use std::net::{SocketAddr, TcpListener, TcpStream};
use std::path::{Path, PathBuf};
use std::process::{Child, Command, Stdio};
use std::sync::Mutex;
use std::thread;
use std::time::Duration;

use tauri::AppHandle;

pub struct BackendHandle {
    base_url: String,
    child: Mutex<Option<Child>>,
}

impl BackendHandle {
    pub fn base_url(&self) -> &str {
        &self.base_url
    }
}

impl Drop for BackendHandle {
    fn drop(&mut self) {
        if let Ok(mut child_guard) = self.child.lock() {
            if let Some(mut child) = child_guard.take() {
                let _ = child.kill();
                let _ = child.wait();
            }
        }
    }
}

pub fn start_backend(app: AppHandle) -> Result<BackendHandle, io::Error> {
    let backend_root = resolve_backend_root(&app)?;
    let port = allocate_port()?;
    let java_executable = resolve_java_executable(&backend_root);
    let classpath = build_classpath(&backend_root);
    let mut log_file = open_startup_log_file()?;

    log_startup_line(
        &mut log_file,
        &format!(
            "Starting backend. root={}, java={}, port={}",
            backend_root.display(),
            java_executable.display(),
            port
        ),
    )?;

    let mut command = Command::new(java_executable);
    let stdout_log = log_file.try_clone()?;
    let stderr_log = log_file.try_clone()?;
    command
        .current_dir(&backend_root)
        .arg("-cp")
        .arg(classpath)
        .arg("com.dbsyncstudio.core.DbSyncStudioApplication")
        .arg("--server")
        .arg(format!("--port={}", port))
        .stdin(Stdio::null())
        .stdout(Stdio::from(stdout_log))
        .stderr(Stdio::from(stderr_log));

    let mut child = match command.spawn() {
        Ok(child) => child,
        Err(err) => {
            let _ = log_startup_line(&mut log_file, &format!("Failed to launch backend: {}", err));
            return Err(io::Error::new(io::ErrorKind::Other, format!("Failed to launch backend: {}", err)));
        }
    };

    if let Err(err) = wait_for_port(port) {
        let _ = log_startup_line(
            &mut log_file,
            &format!("Backend failed to start on port {}: {}", port, err),
        );
        let _ = child.kill();
        let _ = child.wait();
        return Err(err);
    }

    Ok(BackendHandle {
        base_url: format!("http://127.0.0.1:{}", port),
        child: Mutex::new(Some(child)),
    })
}

fn resolve_backend_root(app: &AppHandle) -> Result<PathBuf, io::Error> {
    let backend_root = app
        .path_resolver()
        .resolve_resource("backend")
        .ok_or_else(|| io::Error::new(io::ErrorKind::NotFound, "Backend resources were not found"))?;
    if !backend_root.exists() {
        return Err(io::Error::new(
            io::ErrorKind::NotFound,
            format!("Backend resource directory not found: {}", backend_root.display()),
        ));
    }
    Ok(backend_root)
}

fn resolve_java_executable(backend_root: &Path) -> PathBuf {
    let runtime_bin = backend_root.join("runtime").join("bin");
    if cfg!(target_os = "windows") {
        let candidate = runtime_bin.join("java.exe");
        if candidate.exists() {
            return candidate;
        }
    } else {
        let candidate = runtime_bin.join("java");
        if candidate.exists() {
            return candidate;
        }
    }

    PathBuf::from("java")
}

fn build_classpath(backend_root: &Path) -> String {
    let jar = backend_root.join("app-core.jar");
    let lib_dir = backend_root.join("lib");
    if cfg!(target_os = "windows") {
        format!("{};{}\\*", jar.display(), lib_dir.display())
    } else {
        format!("{}:{}/*", jar.display(), lib_dir.display())
    }
}

fn allocate_port() -> Result<u16, io::Error> {
    TcpListener::bind("127.0.0.1:0")
        .map_err(|err| io::Error::new(io::ErrorKind::Other, format!("Failed to allocate backend port: {}", err)))
        .and_then(|listener| {
            let port = listener
                .local_addr()
                .map_err(|err| io::Error::new(io::ErrorKind::Other, format!("Failed to read backend port: {}", err)))?
                .port();
            drop(listener);
            Ok(port)
        })
}

fn wait_for_port(port: u16) -> Result<(), io::Error> {
    let addr = SocketAddr::from(([127, 0, 0, 1], port));
    for _ in 0..100 {
        if TcpStream::connect(addr).is_ok() {
            return Ok(());
        }
        thread::sleep(Duration::from_millis(50));
    }
    Err(io::Error::new(
        io::ErrorKind::TimedOut,
        format!("Backend did not start on port {}", port),
    ))
}

pub fn append_startup_log_line(message: &str) {
    if let Ok(mut file) = open_startup_log_file() {
        let _ = log_startup_line(&mut file, message);
    }
}

fn open_startup_log_file() -> Result<File, io::Error> {
    let log_file_path = startup_log_path();
    if let Some(parent) = log_file_path.parent() {
        create_dir_all(parent)?;
    }
    OpenOptions::new()
        .create(true)
        .append(true)
        .open(log_file_path)
}

fn log_startup_line(file: &mut File, message: &str) -> Result<(), io::Error> {
    writeln!(file, "{}", message)?;
    file.flush()
}

fn startup_log_path() -> PathBuf {
    resolve_app_directory().join("logs").join("startup.log")
}

fn resolve_app_directory() -> PathBuf {
    let user_home = env::var_os("USERPROFILE")
        .or_else(|| env::var_os("HOME"))
        .map(PathBuf::from)
        .unwrap_or_else(|| PathBuf::from("."));
    if cfg!(target_os = "windows") {
        if let Some(appdata) = env::var_os("APPDATA") {
            return PathBuf::from(appdata).join(".db-sync-studio");
        }
        return user_home.join("AppData").join("Roaming").join(".db-sync-studio");
    }
    if cfg!(target_os = "macos") {
        return user_home.join("Library").join("Application Support").join(".db-sync-studio");
    }
    if let Some(xdg_data_home) = env::var_os("XDG_DATA_HOME") {
        return PathBuf::from(xdg_data_home).join(".db-sync-studio");
    }
    user_home.join(".local").join("share").join(".db-sync-studio")
}
