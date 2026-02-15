# EchoWave – Real-Time Walkie-Talkie Web Communication System

[![Java](https://img.shields.io/badge/Java-17+-007396?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![WebRTC](https://img.shields.io/badge/WebRTC-Peer--to--Peer-333333?style=for-the-badge&logo=webrtc)](https://webrtc.org)
[![Redis](https://img.shields.io/badge/Redis-8.x-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

**EchoWave** is a modern **browser-based walkie-talkie** application that enables real-time, low-latency push-to-talk voice communication — no downloads or apps required.

It replicates classic radio behavior: **only one person can speak at a time**, others listen instantly, with seamless speaker handoff and online presence tracking.

**Live Demo**: [https://www.echowavevoices.com](https://www.echowavevoices.com)  
**GitHub**: [https://github.com/sarthakmaiti/echowave](https://github.com/sarthakmaiti/echowave)

## ✨ Key Features

- **Push-to-Talk** voice (hold button to speak, release to stop)
- **Single active speaker** enforcement using Redis distributed lock
- **Real-time user presence** — see who's online in the channel
- **WebRTC peer-to-peer** audio streaming (no central media server)
- **Secure authentication** with JWT
- **Cross-browser** support (Chrome, Firefox, Edge recommended)
- **Containerized** deployment with Docker

## 🛠 Tech Stack

- **Backend**: Java 17+, Spring Boot 3, WebSocket (STOMP), JWT, Redis
- **Frontend**: HTML5, CSS3, Vanilla JavaScript (ES6+), WebRTC
- **Real-time Audio**: WebRTC (peer-to-peer)
- **Caching & Concurrency**: Redis (presence via Sets, speaker lock via SETNX + TTL)
- **Containerization**: Docker
- **Deployment**: AWS EC2

## 🚀 Quick Start (Local Development)

### Prerequisites

- Java 17+
- Maven
- Redis server (local or Docker)
- Modern browser (Chrome/Firefox recommended)

### 1. Clone the repository

```bash
git clone https://github.com/sarthakmaiti/EchoWave.git
cd EchoWave
