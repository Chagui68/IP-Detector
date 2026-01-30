# 🔐 SingleIP Plugin

![Minecraft](https://img.shields.io/badge/minecraft-1.20.6-green)
![Java](https://img.shields.io/badge/java-21-orange)
![License](https://img.shields.io/badge/license-MIT-yellow)
> A security plugin for Minecraft that restricts player access to registered IP addresses


---

## 📖 Description

SingleIP is a security plugin for Spigot/Paper servers developed and tested on **Minecraft 1.20.6** with **Java 21** that allows you to:
- Limit player access to specific registered IP addresses
- Prevent unauthorized access from unknown IPs
- Automatically register the first connection IP
- Manage multiple IPs per player (configurable)
- Monitor failed login attempts

**Ideal for**: Private servers, servers with shared accounts, account theft prevention

---

## ✨ Features


- ✅ Automatic first IP registration
- ✅ Unlimited configurable IPs per player (no hardcoded limits)
- ✅ IPv4 and IPv6 validation
- ✅ Failed login attempts logging system
- ✅ Console-only administrative commands
- ✅ YAML file for data management
- ✅ Customizable kick messages
- ✅ Reload system without server restart
- ✅ Multi-version Minecraft support

---

## 📋 Requirements

- Minecraft Server: **1.20.6** (tested)  
- API: **Spigot/Paper**
- Java: **21** (recommended) 

---

## 🚀 Installation

1. Download the `.jar` file from [Releases](../../releases)
2. Place the file in your server's `plugins/` folder
3. Restart the server
4. Configure the plugin by editing `plugins/SingleIP/config.yml`
5. Restart again or use `/ipmanager reload`


