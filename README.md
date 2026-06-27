# pingbypass

Minecraft 1.21.4 PingBypass for HeadlessMC.

This project runs a second Minecraft client on a VPS and lets your normal client connect through it. The point is simple: modules that care about latency can execute from the VPS side, closer to the server, while you still play from your own PC.

The server-side setup is handled by [`script.sh`](script.sh) ([raw GitHub file](https://github.com/godmoduleu/pingbypass/blob/main/script.sh)). It builds a Docker image, installs/starts HeadlessMC with Fabric, copies the PingBypass jar into the container, runs the Microsoft device-code login, and starts a small menu for managing the proxy.

![CrystalPvP screenshot](images/cc.png)

## Status

- Minecraft: `1.21.4`
- Loader: Fabric
- Runtime: HeadlessMC in Docker
- Tested on: `crystalpvp.cc`
- Known working target style: CrystalPvP / Grim servers such as `crystalpvp.cc` and `grim.crystalpvp.cc`
- Not tested on: `2b2t` and similar large anarchy servers

If you try it on a server that is not listed here, treat it as untested.

## Requirements

Use a Linux VPS. Debian or Ubuntu is the easiest path because the script can install Docker and the basic tools automatically with `apt`.

You need:

- a Minecraft account for the HeadlessMC login
- the PingBypass client jar
- `script.sh`
- root/sudo access on the VPS
- the VPS port you choose opened in your firewall/provider panel

Important: keep the client jar and `script.sh` in the same folder before you run setup. Do not run the script from one directory while the jar is somewhere else. The setup script searches nearby jar files and copies the selected Fabric mod jar into the Docker build as `build/libs/astera.jar`.

## Quick Start

Put `script.sh` and your PingBypass jar in one directory on the VPS:

```bash
ls
```

Example:

```text
script.sh
pingbypass.jar
```

Run the menu:

```bash
sudo bash script.sh
```

Choose `Setup / Install`.

During setup the script asks for:

- your PingBypass license key, if one is not already saved
- language
- PingBypass password
- public VPS port, default `25565`
- JVM memory, default `2G`
- Minecraft Microsoft login through HeadlessMC

When setup finishes, the script prints the connection info:

```text
Host/IP : your.vps.ip
Port    : 25565
Pass    : your-pb-password
```

Add the VPS IP and port as a server in your local Minecraft client. Use the same PingBypass jar on the client side.

## How The Script Works

`script.sh` is the server manager for this project. It does not pick the final Minecraft server for you.

What it does:

- verifies the PingBypass license
- installs Docker on apt-based systems if Docker is missing
- finds a Fabric mod jar in the script directory
- downloads Minecraft `1.21.4`, Fabric, and Fabric API inside the Docker image
- creates the server-side PingBypass config
- logs the HeadlessMC client into Minecraft
- starts a Docker container named `pingbypass-server`
- exposes the selected VPS port to the PingBypass server
- provides start, stop, restart, logs, connection info, language, and AntiBot/NotBot help menus

The target server is chosen from the client side. The headless client connects to that target after your local client has connected to the PingBypass server and sent the join request.

## Client Notes

Use Minecraft `1.21.4` with Fabric and the PingBypass jar.

The client connects to your VPS first. The VPS-side HeadlessMC session then connects onward to the real Minecraft server, for example:

```text
crystalpvp.cc
grim.crystalpvp.cc
```

Only `crystalpvp.cc` has been tested directly in this repo state.

## Menu

After the first install, run the script again whenever you want to manage the server:

```bash
sudo bash script.sh
```

Menu options:

- `Setup / Install` rebuilds the image, refreshes config, logs in if needed, and starts the container
- `Start` starts an existing stopped container
- `Stop` stops the running container
- `Restart` restarts it
- `View Logs` follows Docker logs
- `Connection Info` shows the public IP, port, password mask, image, and container status
- `AntiBot / NotBot Help` prints SOCKS proxy commands for manual verification from the VPS IP
- `Change Language` switches the script language

## AntiBot / NotBot

The script does not solve captchas.

For servers that require browser verification, use the menu's AntiBot / NotBot helper. It shows an SSH SOCKS proxy command so your browser verification page sees the VPS IP. Keep the SSH proxy open while verifying, check that `api.ipify.org` shows the VPS IP, then complete the verification manually.

## Build From Source

If you want to build the jar yourself:

```bash
./gradlew build
```

The mod jar is produced under:

```text
build/libs/
```

For server setup, place that jar next to `script.sh` before running the installer.

## Credits

Client developers:

- HumanBean
- suposoe
- [Tinkoprof](https://github.com/tinkoprofplus)

Client base:

- [Sydney Legacy](https://github.com/sydney-client/Sydney-Legacy)

Server-side manager and PingBypass packaging:

- godmodule

## License

This project is licensed under MIT. See [`LICENSE`](LICENSE).
