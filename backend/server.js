/**
 * Neiro PC Music Streaming Server
 * 
 * 초경량 Node.js (Express) 음악 스트리밍 백엔드 단일 파일
 * - PC 로컬 음악 폴더를 재귀적으로 스캔
 * - ID3 / FLAC / M4A 메타데이터 자동 추출 (제목, 아티스트, 앨범, 재생시간)
 * - 앨범 커버 아트 바이너리 추출 및 캐싱 서빙 (/api/cover/:id)
 * - 가사 서빙 (/api/lyrics/:id): 동명 .lrc 파일 및 파일 내부 임베디드 가사 파싱 지원
 * - 모바일 스트리밍 최적화: HTTP 206 Partial Content (Range 헤더) 완전 지원 (시크바 즉각 반응)
 * - CORS 허용 (모바일 앱 / 브라우저 접속 지원)
 * 
 * 실행 방법:
 *   1) npm install
 *   2) node server.js "음악폴더경로" [포트번호]
 *      예: node server.js "D:\\Music" 3000
 *      또는 환경변수: MUSIC_DIR="D:\\Music" PORT=3000 node server.js
 */

const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const os = require('os');
const mm = require('music-metadata');

const app = express();

// 1. 설정 (CLI 인자 또는 환경변수)
const MUSIC_DIR = process.argv[2] || process.env.MUSIC_DIR || path.join(os.homedir(), 'Music');
const PORT = parseInt(process.argv[3] || process.env.PORT || '3000', 10);
const SUPPORTED_EXTS = new Set(['.mp3', '.flac', '.m4a', '.aac', '.ogg', '.wav', '.opus']);

app.use(cors());
app.use(express.json());

// 인메모리 메타데이터 저장소
let songList = [];
let songMap = new Map(); // id -> song detail
let isScanning = false;

// 로컬 네트워크 IP 확인 헬퍼 (스마트폰 접속용)
function getLocalIpAddresses() {
  const interfaces = os.networkInterfaces();
  const addresses = [];
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        addresses.push(iface.address);
      }
    }
  }
  return addresses;
}

// LRC 가사 파일 파서 ([mm:ss.xx] 가사 -> { time: 초, text: '가사' })
function parseLrc(lrcText) {
  const lines = lrcText.split(/\r?\n/);
  const result = [];
  const timeRegex = /\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\]/g;

  for (const line of lines) {
    timeRegex.lastIndex = 0;
    const matches = [...line.matchAll(timeRegex)];
    if (matches.length > 0) {
      const text = line.replace(timeRegex, '').trim();
      for (const match of matches) {
        const min = parseInt(match[1], 10);
        const sec = parseInt(match[2], 10);
        const ms = match[3] ? parseInt(match[3].padEnd(3, '0').slice(0, 3), 10) : 0;
        const timeInSeconds = min * 60 + sec + ms / 1000;
        result.push({ time: timeInSeconds, text });
      }
    }
  }

  // 시간순 정렬
  result.sort((a, b) => a.time - b.time);
  return result;
}

// 파일 스캔 및 메타데이터 수집
async function scanMusicFolder(dir) {
  if (isScanning) return;
  isScanning = true;
  console.log(`\n📂 [스캔 시작] 음악 폴더: "${dir}"`);

  if (!fs.existsSync(dir)) {
    console.error(`❌ [오류] 경로를 찾을 수 없습니다: ${dir}`);
    try {
      fs.mkdirSync(dir, { recursive: true });
      console.log(`📁 폴더를 생성했습니다: ${dir}`);
    } catch (e) {
      console.error(e);
    }
  }

  const foundFiles = [];
  function collectFiles(currentDir) {
    try {
      const entries = fs.readdirSync(currentDir, { withFileTypes: true });
      for (const entry of entries) {
        if (entry.name.startsWith('.')) continue; // 숨김 파일/폴더 제외
        const fullPath = path.join(currentDir, entry.name);
        if (entry.isDirectory()) {
          collectFiles(fullPath);
        } else if (entry.isFile()) {
          const ext = path.extname(entry.name).toLowerCase();
          if (SUPPORTED_EXTS.has(ext)) {
            foundFiles.push(fullPath);
          }
        }
      }
    } catch (err) {
      console.warn(`⚠️ 폴더 읽기 경고: ${currentDir}`, err.message);
    }
  }

  collectFiles(dir);
  console.log(`🎶 총 ${foundFiles.length}개의 음원 파일을 발견했습니다. 메타데이터 파싱 중...`);

  const newSongList = [];
  const newSongMap = new Map();

  for (let i = 0; i < foundFiles.length; i++) {
    const filePath = foundFiles[i];
    const id = `song_${i + 1}`;
    const filename = path.basename(filePath);
    const ext = path.extname(filePath).toLowerCase();

    let title = path.basename(filePath, ext);
    let artist = 'Unknown Artist';
    let album = 'Unknown Album';
    let duration = 0;
    let trackNo = null;
    let year = null;
    let hasCover = false;
    let coverBuffer = null;
    let coverMime = null;
    let embeddedLyrics = null;

    try {
      const metadata = await mm.parseFile(filePath, { skipCovers: false });
      const common = metadata.common || {};
      const format = metadata.format || {};

      if (common.title && common.title.trim()) title = common.title.trim();
      if (common.artist && common.artist.trim()) artist = common.artist.trim();
      if (common.album && common.album.trim()) album = common.album.trim();
      if (format.duration) duration = Math.round(format.duration);
      if (common.track && common.track.no) trackNo = common.track.no;
      if (common.year) year = common.year;

      // 앨범 커버 확인
      if (common.picture && common.picture.length > 0) {
        hasCover = true;
        coverBuffer = common.picture[0].data;
        coverMime = common.picture[0].format || 'image/jpeg';
      }

      // 내장 가사 태그 확인
      if (common.lyrics && common.lyrics.length > 0) {
        embeddedLyrics = common.lyrics[0].text || common.lyrics[0];
      }
    } catch (err) {
      // 메타데이터 읽기 실패 시 파일명으로 기본값 사용
    }

    // 동명 .lrc 가사 파일 존재 여부 확인
    const lrcPath = filePath.slice(0, -ext.length) + '.lrc';
    let hasLrc = false;
    let lrcContent = null;

    if (fs.existsSync(lrcPath)) {
      hasLrc = true;
      try {
        lrcContent = fs.readFileSync(lrcPath, 'utf-8');
      } catch (e) {}
    }

    const song = {
      id,
      title,
      artist,
      album,
      duration, // 초 단위
      trackNo,
      year,
      filename,
      filePath,
      hasCover,
      coverBuffer,
      coverMime,
      hasLyrics: hasLrc || Boolean(embeddedLyrics),
      lrcContent: lrcContent || embeddedLyrics
    };

    newSongList.push({
      id: song.id,
      title: song.title,
      artist: song.artist,
      album: song.album,
      duration: song.duration,
      trackNo: song.trackNo,
      year: song.year,
      hasCover: song.hasCover,
      hasLyrics: song.hasLyrics
    });

    newSongMap.set(id, song);
  }

  songList = newSongList;
  songMap = newSongMap;
  isScanning = false;
  console.log(`✅ [스캔 완료] 총 ${songList.length}곡 준비 완료!\n`);
}

// ================= API 엔드포인트 =================

// 1. 서버 상태 및 네트워크 정보
app.get('/api/status', (req, res) => {
  res.json({
    status: 'online',
    musicDir: MUSIC_DIR,
    songCount: songList.length,
    isScanning,
    localIps: getLocalIpAddresses().map(ip => `http://${ip}:${PORT}`)
  });
});

// 2. 전체 음원 목록
app.get('/api/songs', (req, res) => {
  res.json({
    total: songList.length,
    songs: songList
  });
});

// 3. 특정 곡 상세 정보
app.get('/api/songs/:id', (req, res) => {
  const song = songMap.get(req.params.id);
  if (!song) return res.status(404).json({ error: 'Song not found' });

  res.json({
    id: song.id,
    title: song.title,
    artist: song.artist,
    album: song.album,
    duration: song.duration,
    trackNo: song.trackNo,
    year: song.year,
    hasCover: song.hasCover,
    hasLyrics: song.hasLyrics
  });
});

// 4. 앨범 커버 이미지 서빙
app.get('/api/cover/:id', (req, res) => {
  const song = songMap.get(req.params.id);
  if (!song || !song.hasCover || !song.coverBuffer) {
    return res.status(404).send('Cover image not found');
  }

  res.setHeader('Content-Type', song.coverMime || 'image/jpeg');
  res.setHeader('Cache-Control', 'public, max-age=86400'); // 24시간 캐시
  res.send(song.coverBuffer);
});

// 5. 가사 서빙 (타임스탬프 파싱 결과 포함)
app.get('/api/lyrics/:id', (req, res) => {
  const song = songMap.get(req.params.id);
  if (!song || !song.hasLyrics || !song.lrcContent) {
    return res.status(404).json({ error: 'Lyrics not found' });
  }

  const rawLyrics = song.lrcContent;
  const parsedLines = parseLrc(rawLyrics);

  res.json({
    id: song.id,
    isSynced: parsedLines.length > 0,
    raw: rawLyrics,
    lines: parsedLines // [{ time: 12.5, text: "..." }]
  });
});

// 6. 음원 스트리밍 (HTTP 206 Partial Content / Range 요청 완벽 지원)
app.get('/api/stream/:id', (req, res) => {
  const song = songMap.get(req.params.id);
  if (!song) return res.status(404).json({ error: 'Song not found' });

  const filePath = song.filePath;
  if (!fs.existsSync(filePath)) {
    return res.status(404).json({ error: 'File missing on server' });
  }

  const stat = fs.statSync(filePath);
  const fileSize = stat.size;
  const range = req.headers.range;

  const ext = path.extname(filePath).toLowerCase();
  const mimeTypes = {
    '.mp3': 'audio/mpeg',
    '.flac': 'audio/flac',
    '.m4a': 'audio/mp4',
    '.aac': 'audio/aac',
    '.ogg': 'audio/ogg',
    '.wav': 'audio/wav',
    '.opus': 'audio/opus'
  };
  const contentType = mimeTypes[ext] || 'audio/mpeg';

  if (range) {
    // Range 헤더 처리 (예: "bytes=1048576-")
    const parts = range.replace(/bytes=/, "").split("-");
    const start = parseInt(parts[0], 10);
    const end = parts[1] ? parseInt(parts[1], 10) : fileSize - 1;

    if (start >= fileSize || end >= fileSize) {
      res.status(416).set('Content-Range', `bytes */${fileSize}`).end();
      return;
    }

    const chunksize = (end - start) + 1;
    const stream = fs.createReadStream(filePath, { start, end });

    res.writeHead(206, {
      'Content-Range': `bytes ${start}-${end}/${fileSize}`,
      'Accept-Ranges': 'bytes',
      'Content-Length': chunksize,
      'Content-Type': contentType,
      'Cache-Control': 'no-cache'
    });

    stream.pipe(res);
  } else {
    // 전체 파일 전송
    res.writeHead(200, {
      'Content-Length': fileSize,
      'Content-Type': contentType,
      'Accept-Ranges': 'bytes'
    });

    fs.createReadStream(filePath).pipe(res);
  }
});

// 7. 검색 API (제목, 아티스트, 앨범)
app.get('/api/search', (req, res) => {
  const query = (req.query.q || '').toLowerCase().trim();
  if (!query) {
    return res.json({ songs: songList });
  }

  const filtered = songList.filter(s => 
    s.title.toLowerCase().includes(query) ||
    s.artist.toLowerCase().includes(query) ||
    s.album.toLowerCase().includes(query)
  );

  res.json({ songs: filtered });
});

// 8. 폴더 다시 스캔
app.post('/api/scan', async (req, res) => {
  if (isScanning) {
    return res.status(409).json({ message: 'Scanning already in progress' });
  }
  scanMusicFolder(MUSIC_DIR);
  res.json({ message: 'Scan started' });
});

// 서버 기동
app.listen(PORT, '0.0.0.0', () => {
  console.log(`
======================================================
  🎵 Neiro Music Streaming Server 기동 완료!
======================================================
  - 스캔 폴더: ${MUSIC_DIR}
  - 로컬 접속:  http://localhost:${PORT}
  - 모바일 접속 주소 (동일 Wi-Fi 내 스마트폰에서 접속):`);

  const ips = getLocalIpAddresses();
  if (ips.length === 0) {
    console.log(`    (Wi-Fi 네트워크에 연결되어 있는지 확인해 주세요)`);
  } else {
    ips.forEach(ip => {
      console.log(`    👉 http://${ip}:${PORT}`);
    });
  }

  console.log(`======================================================\n`);

  // 초기 폴더 스캔 실행
  scanMusicFolder(MUSIC_DIR);
});
