use std::io;
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

    let mut command = Command::new(java_executable);
    command
        .current_dir(&backend_root)
        .arg("-cp")
        .arg(classpath)
        .arg("com.dbsyncstudio.core.DbSyncStudioApplication")
        .arg("--server")
        .arg(format!("--port={}", port))
        .stdin(Stdio::null())
        .stdout(Stdio::null())
        .stderr(Stdio::null());

    let child = command.spawn().map_err(|err| io::Error::new(io::ErrorKind::Other, format!("Failed to launch backend: {}", err)))?;
    wait_for_port(port)?;

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
