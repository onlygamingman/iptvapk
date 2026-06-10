<?php
/*
|--------------------------------------------------------------------------
| Live Sports Admin Panel Dashboard (admin.php)
|--------------------------------------------------------------------------
| This file provides a premium web UI to manage matches, channels, and configs.
| Security: Simple username & password session based auth is included.
| No MySQL database needed; saves everything directly to data.json.
*/

session_start();

// Admin Authentication Configuration
define('ADMIN_USER', 'admin');
define('ADMIN_PASS', '123456'); // Change this password for your security!

$dbFile = 'data.json';

// Initialize data if not exist (same check as api.php)
if (!file_exists($dbFile)) {
    $defaultData = [
        'config' => [
            'adsEnabled' => true,
            'bannerAdUrl' => 'https://example.com',
            'popUnderUrl' => 'https://example.com',
            'bannerAdCode' => '',
            'popUnderCode' => '',
            'showNotice' => true,
            'noticeTitle' => 'KhelaGhor LIVE',
            'noticeMessage' => 'স্বাগতম খেলাঘর অ্যাপে! লাইভ খেলা দেখতে আমাদের সাথেই থাকুন।',
            'noticeButtonText' => 'টেলিগ্রাম গ্রুপ',
            'noticeLink' => 'https://telegram.me/'
        ],
        'matches' => [],
        'channels' => []
    ];
    file_put_contents($dbFile, json_encode($defaultData, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
}

// Load current data
$data = json_decode(file_get_contents($dbFile), true);

// Auth actions
if (isset($_POST['login'])) {
    $user = $_POST['username'] ?? '';
    $pass = $_POST['password'] ?? '';
    if ($user === ADMIN_USER && $pass === ADMIN_PASS) {
        $_SESSION['loggedin'] = true;
        $success_msg = "সফলভাবে লগইন হয়েছে!";
    } else {
        $error_msg = "ভুল ইউজারনেম বা পাসওয়ার্ড!";
    }
}

if (isset($_GET['logout'])) {
    session_destroy();
    header("Location: admin.php");
    exit();
}

$isLoggedIn = isset($_SESSION['loggedin']) && $_SESSION['loggedin'] === true;

// Save operations (Only when logged in)
if ($isLoggedIn) {
    // 1. Update Config Form
    if (isset($_POST['update_config'])) {
        $data['config']['adsEnabled'] = isset($_POST['adsEnabled']);
        $data['config']['bannerAdUrl'] = $_POST['bannerAdUrl'] ?? '';
        $data['config']['popUnderUrl'] = $_POST['popUnderUrl'] ?? '';
        $data['config']['bannerAdCode'] = $_POST['bannerAdCode'] ?? '';
        $data['config']['popUnderCode'] = $_POST['popUnderCode'] ?? '';
        $data['config']['showNotice'] = isset($_POST['showNotice']);
        $data['config']['noticeTitle'] = $_POST['noticeTitle'] ?? '';
        $data['config']['noticeMessage'] = $_POST['noticeMessage'] ?? '';
        $data['config']['noticeButtonText'] = $_POST['noticeButtonText'] ?? '';
        $data['config']['noticeLink'] = $_POST['noticeLink'] ?? '';
        
        file_put_contents($dbFile, json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
        $success_msg = "অ্যাপ কনফিগারেশন সেভ হয়েছে!";
    }

    // 2. Add Match
    if (isset($_POST['add_match'])) {
        $newMatch = [
            'category' => $_POST['category'] ?: 'Cricket',
            'team1Name' => $_POST['team1Name'] ?: 'Team A',
            'team1LogoUrl' => $_POST['team1LogoUrl'] ?? '',
            'team2Name' => $_POST['team2Name'] ?: 'Team B',
            'team2LogoUrl' => $_POST['team2LogoUrl'] ?? '',
            'streamUrl' => $_POST['streamUrl'] ?? '',
            'tournament' => $_POST['tournament'] ?: 'LIVE Tournament',
            'status' => $_POST['status'] ?: 'LIVE',
            'startTimeStamp' => (int)($_POST['startTimeStamp'] ?: (time() * 1000))
        ];
        $data['matches'][] = $newMatch;
        file_put_contents($dbFile, json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
        $success_msg = "নতুন ম্যাচ যুক্ত হয়েছে!";
    }

    // 3. Delete Match
    if (isset($_GET['delete_match'])) {
        $index = (int)$_GET['delete_match'];
        if (isset($data['matches'][$index])) {
            array_splice($data['matches'], $index, 1);
            file_put_contents($dbFile, json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
            $success_msg = "ম্যাচ ডিলিট করা হয়েছে!";
        }
    }

    // 4. Add Channel
    if (isset($_POST['add_channel'])) {
        $newChannel = [
            'categoryName' => $_POST['categoryName'] ?: 'Sports Channels',
            'channelName' => $_POST['channelName'] ?: 'New Channel',
            'channelLogoUrl' => $_POST['channelLogoUrl'] ?? '',
            'streamUrl' => $_POST['streamUrl'] ?? ''
        ];
        $data['channels'][] = $newChannel;
        file_put_contents($dbFile, json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
        $success_msg = "নতুন চ্যানেল যুক্ত হয়েছে!";
    }

    // 5. Delete Channel
    if (isset($_GET['delete_channel'])) {
        $index = (int)$_GET['delete_channel'];
        if (isset($data['channels'][$index])) {
            array_splice($data['channels'], $index, 1);
            file_put_contents($dbFile, json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
            $success_msg = "চ্যানেল ডিলিট করা হয়েছে!";
        }
    }
}
?>
<!DOCTYPE html>
<html lang="bn">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>খেলাঘর লাইভ স্পোর্টস - অ্যাডমিন প্যানেল</title>
    <!-- Tailwind CSS CDN -->
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Hind+Siliguri:wght@400;600;700&display=swap');
        body {
            font-family: 'Hind Siliguri', sans-serif;
            background-color: #0b1c10;
        }
    </style>
</head>
<body class="text-white min-h-screen">

    <!-- Header Navbar -->
    <nav class="bg-[#050e08] border-b border-green-950/70 p-4">
        <div class="container mx-auto flex justify-between items-center sm:px-6">
            <h1 class="text-xl font-bold text-green-400 flex items-center gap-2">
                <i class="fa-solid fa-gamepad text-emerald-400"></i>
                খেলাঘর - <span class="text-white">অ্যাডমিন প্যানেল</span>
            </h1>
            <?php if ($isLoggedIn): ?>
                <a href="?logout=true" class="bg-red-600 hover:bg-red-700 text-white font-bold py-1.5 px-4 rounded text-sm transition">
                    <i class="fa-solid fa-right-from-bracket mr-1"></i> লগআউট
                </a>
            <?php endif; ?>
        </div>
    </nav>

    <div class="container mx-auto px-4 py-8 max-w-6xl">

        <!-- Banner Alert notifications -->
        <?php if (isset($success_msg)): ?>
            <div class="mb-6 bg-emerald-950 border border-emerald-500 text-emerald-200 px-4 py-3 rounded relative flex items-center gap-2">
                <i class="fa-solid fa-circle-check text-emerald-400 text-lg"></i>
                <div><?php echo $success_msg; ?></div>
            </div>
        <?php endif; ?>
        
        <?php if (isset($error_msg)): ?>
            <div class="mb-6 bg-red-950 border border-red-500 text-red-200 px-4 py-3 rounded relative flex items-center gap-2">
                <i class="fa-solid fa-circle-exclamation text-red-400 text-lg"></i>
                <div><?php echo $error_msg; ?></div>
            </div>
        <?php endif; ?>


        <!-- LOGIN SCREEN -->
        <?php if (!$isLoggedIn): ?>
            <div class="max-w-md mx-auto bg-[#07130a] border border-green-900 rounded-xl p-8 shadow-2xl mt-12">
                <div class="text-center mb-6">
                    <div class="w-16 h-16 bg-emerald-950 text-emerald-400 rounded-full flex items-center justify-center mx-auto mb-4 text-2xl border border-emerald-800">
                        <i class="fa-solid fa-lock"></i>
                    </div>
                    <h2 class="text-2xl font-bold">লগইন প্যানেল</h2>
                    <p class="text-sm text-green-600">খেলাঘর অ্যাপ ডাটা কন্ট্রোল সিস্টেমে প্রবেশ করুন</p>
                </div>

                <form method="POST" action="">
                    <div class="mb-4">
                        <label class="block text-sm text-gray-300 mb-2">ইউজারনেম</label>
                        <input type="text" name="username" class="w-full bg-black border border-green-900 rounded p-2.5 text-white focus:outline-none focus:border-green-400" required>
                    </div>

                    <div class="mb-6">
                        <label class="block text-sm text-gray-300 mb-2">পাসওয়ার্ড</label>
                        <input type="password" name="password" class="w-full bg-black border border-green-900 rounded p-2.5 text-white focus:outline-none focus:border-green-400" required>
                    </div>

                    <button type="submit" name="login" class="w-full bg-[#10b981] hover:bg-[#059669] text-black font-bold py-3 px-4 rounded-lg transition">
                        লগইন করুন
                    </button>
                </form>
                
                <div class="mt-6 text-center text-xs text-gray-500 border-t border-green-950/60 pt-4">
                    ডিফল্ট পাসওয়ার্ড: <span class="text-emerald-500 font-mono">admin / 123456</span>
                </div>
            </div>


        <!-- LOGGED IN USER ADMIN SYSTEM -->
        <?php else: ?>

            <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
                
                <!-- Left Column: Controls & Notifications -->
                <div class="lg:col-span-1 space-y-8">
                    
                    <!-- Core Config / Setup Notice Card -->
                    <div class="bg-[#07130a] border border-green-900/60 rounded-xl p-6 shadow-xl">
                        <h3 class="text-lg font-bold text-green-400 mb-4 border-b border-green-950 pb-2 flex items-center gap-2">
                            <i class="fa-solid fa-gears"></i> অ্যাপ কনফিগারেশন
                        </h3>

                        <form method="POST" action="">
                            <!-- Notice section -->
                            <div class="mb-4">
                                <label class="ui-checkbox flex items-center gap-2 mb-3 cursor-pointer">
                                    <input type="checkbox" name="showNotice" class="accent-emerald-500 w-4 h-4" <?php echo ($data['config']['showNotice'] ?? false) ? 'checked' : ''; ?>>
                                    <span class="text-sm font-semibold">বিজ্ঞপ্তি শো করো (Notice Box)</span>
                                </label>
                            </div>

                            <div class="mb-3">
                                <label class="block text-xs text-gray-400 mb-1">বিজ্ঞপ্তি টাইটেল</label>
                                <input type="text" name="noticeTitle" value="<?php echo htmlspecialchars($data['config']['noticeTitle'] ?? ''); ?>" class="w-full bg-black border border-green-950 rounded p-2 text-sm text-white focus:outline-none focus:border-green-400">
                            </div>

                            <div class="mb-3">
                                <label class="block text-xs text-gray-400 mb-1">বিজ্ঞপ্তির বর্ণনা</label>
                                <textarea name="noticeMessage" rows="3" class="w-full bg-black border border-green-950 rounded p-2 text-sm text-white focus:outline-none focus:border-green-400"><?php echo htmlspecialchars($data['config']['noticeMessage'] ?? ''); ?></textarea>
                            </div>

                            <div class="grid grid-cols-2 gap-2 mb-4">
                                <div>
                                    <label class="block text-xs text-gray-400 mb-1">বাটনের লেখা</label>
                                    <input type="text" name="noticeButtonText" value="<?php echo htmlspecialchars($data['config']['noticeButtonText'] ?? ''); ?>" class="w-full bg-black border border-green-950 rounded p-2 text-xs text-white focus:outline-none focus:border-green-400">
                                </div>
                                <div>
                                    <label class="block text-xs text-gray-400 mb-1">বাটন লিংক</label>
                                    <input type="url" name="noticeLink" value="<?php echo htmlspecialchars($data['config']['noticeLink'] ?? ''); ?>" class="w-full bg-black border border-green-950 rounded p-2 text-xs text-white focus:outline-none focus:border-green-400">
                                </div>
                            </div>

                            <hr class="border-green-950 mb-4">

                            <!-- Ads management -->
                            <div class="mb-4">
                                <label class="flex items-center gap-2 mb-3 cursor-pointer">
                                    <input type="checkbox" name="adsEnabled" class="accent-emerald-500 w-4 h-4" <?php echo ($data['config']['adsEnabled'] ?? false) ? 'checked' : ''; ?>>
                                    <span class="text-sm font-semibold">বিজ্ঞাপন অন করুন (Ads)</span>
                                </label>
                            </div>

                            <div class="mb-3">
                                <label class="block text-xs text-gray-400 mb-1">ব্যানার এড লিংক (Banner Ad URL)</label>
                                <input type="url" name="bannerAdUrl" value="<?php echo htmlspecialchars($data['config']['bannerAdUrl'] ?? ''); ?>" class="w-full bg-black border border-green-950 rounded p-2 text-xs text-white focus:outline-none focus:border-green-400" placeholder="https://">
                            </div>

                            <div class="mb-4">
                                <label class="block text-xs text-gray-400 mb-1">পপআন্ডার এড লিংক (PopUnder URL)</label>
                                <input type="url" name="popUnderUrl" value="<?php echo htmlspecialchars($data['config']['popUnderUrl'] ?? ''); ?>" class="w-full bg-black border border-green-950 rounded p-2 text-xs text-white focus:outline-none focus:border-green-400" placeholder="https://">
                            </div>

                            <button type="submit" name="update_config" class="w-full bg-emerald-500 hover:bg-emerald-600 text-black font-bold py-2 px-4 rounded text-sm transition">
                                <i class="fa-solid fa-floppy-disk mr-1"></i> কনফিগারেশন সেভ করুন
                            </button>
                        </form>
                    </div>

                    <!-- API Info Screen Link Setup -->
                    <div class="bg-[#050e05] border border-green-950 rounded-xl p-6">
                        <h4 class="text-sm font-bold text-green-300 mb-2">আপনার সিঙ্ক URL</h4>
                        <p class="text-xs text-gray-400 leading-relaxed mb-4">
                            আপনার মোবাইল অ্যাপসে লাইভ ডাটা লোড করার জন্য এই এপিআই-এর সম্পূর্ণ লিংকটি খেলাঘরের সেটিংস বক্সে পেস্ট করে দিন:
                        </p>
                        <div class="bg-black/80 rounded p-2.5 flex items-center justify-between text-xs font-mono text-emerald-400 select-all border border-green-950">
                            <span><?php echo (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://$_SERVER[HTTP_HOST]" . dirname($_SERVER['REQUEST_URI']) . "/api.php"; ?></span>
                        </div>
                    </div>

                </div>

                <!-- Right Column: Add/List Matches and Add/List Channels -->
                <div class="lg:col-span-2 space-y-8">
                    
                    <!-- 1. LIVE MATCHES SECTION -->
                    <div class="bg-[#07130a] border border-green-900/60 rounded-xl p-6 shadow-xl">
                        <div class="md:flex justify-between items-center mb-6 border-b border-green-950 pb-3">
                            <h3 class="text-lg font-bold text-green-400 flex items-center gap-2">
                                <i class="fa-solid fa-play text-red-500"></i> লাইভ ম্যাচসমূহ (Live Matches)
                            </h3>
                            <span class="bg-emerald-950 text-emerald-300 text-xs px-2.5 py-1 rounded border border-emerald-900 mt-2 md:mt-0 inline-block font-sans">
                                মোট ম্যাচ: <span class="font-bold"><?php echo count($data['matches']); ?> টি</span>
                            </span>
                        </div>

                        <!-- Add Match Form Accordion -->
                        <div class="mb-8 bg-black/40 border border-green-950/60 rounded-lg p-4">
                            <h4 class="text-sm font-bold text-green-300 mb-4 flex items-center gap-1.5">
                                <i class="fa-solid fa-plus-circle"></i> নতুন ম্যাচ যুক্ত করুন
                            </h4>

                            <form method="POST" action="">
                                <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                                    <div>
                                        <label class="block text-xs text-gray-400 mb-1">ক্যাটাগরি</label>
                                        <select name="category" class="w-full bg-black border border-green-950 rounded p-2 text-sm text-white focus:outline-none focus:border-green-400">
                                            <option value="Cricket">Cricket</option>
                                            <option value="Football">Football</option>
                                            <option value="Others">Others</option>
                                        </select>
                                    </div>
                                    <div>
                                        <label class="block text-xs text-gray-400 mb-1">টুর্নামেন্টের নাম (যেমন: IPL 2026)</label>
                                        <input type="text" name="tournament" placeholder="T20 World Cup" class="w-full bg-black border border-green-950 rounded p-2 text-sm text-white focus:outline-none focus:border-green-400" required>
                                    </div>
                                    <div>
                                        <label class="block text-xs text-gray-400 mb-1">স্ট্যাটাস (যেমন: LIVE / UPCOMING)</label>
                                        <select name="status" class="w-full bg-black border border-green-950 rounded p-2 text-sm text-white focus:outline-none focus:border-green-400">
                                            <option value="LIVE">LIVE</option>
                                            <option value="UPCOMING">UPCOMING</option>
                                            <option value="ENDED">ENDED</option>
                                        </select>
                                    </div>
                                </div>

                                <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                                    <div class="bg-emerald-950/20 p-3 rounded border border-green-950/50">
                                        <h5 class="text-xs font-bold text-emerald-400 mb-2">দল-১ (টিম ১)</h5>
                                        <div class="space-y-2">
                                            <input type="text" name="team1Name" placeholder="দলের নাম (যেমন: Bangladesh)" class="w-full bg-black border border-green-950 rounded p-1.5 text-xs text-white focus:outline-none focus:border-green-400" required>
                                            <input type="url" name="team1LogoUrl" placeholder="লোগো লিংক (URL)" class="w-full bg-black border border-green-950 rounded p-1.5 text-xs text-white focus:outline-none focus:border-green-400">
                                        </div>
                                    </div>

                                    <div class="bg-emerald-950/20 p-3 rounded border border-green-950/50">
                                        <h5 class="text-xs font-bold text-emerald-400 mb-2">দল-২ (টিম ২)</h5>
                                        <div class="space-y-2">
                                            <input type="text" name="team2Name" placeholder="দলের নাম (যেমন: India)" class="w-full bg-black border border-green-950 rounded p-1.5 text-xs text-white focus:outline-none focus:border-green-400" required>
                                            <input type="url" name="team2LogoUrl" placeholder="লোগো লিংক (URL)" class="w-full bg-black border border-green-950 rounded p-1.5 text-xs text-white focus:outline-none focus:border-green-400">
                                        </div>
                                    </div>
                                </div>

                                <div class="grid grid-cols-2 gap-4 mb-4">
                                    <div class="col-span-2">
                                        <label class="block text-xs text-gray-400 mb-1">স্ট্রিমিং URL (HLS/m3u8, MP4, custom RTMP/Embed URL)</label>
                                        <input type="text" name="streamUrl" placeholder="http://example.com/live/index.m3u8" class="w-full bg-black border border-green-950 rounded p-2 text-sm text-white focus:outline-none focus:border-green-400" required>
                                    </div>
                                </div>

                                <button type="submit" name="add_match" class="bg-emerald-500 hover:bg-emerald-600 text-black font-bold py-2 px-5 rounded text-xs transition flex items-center gap-1">
                                    <i class="fa-solid fa-square-plus"></i> ম্যাচ যুক্ত করুন
                                </button>
                            </form>
                        </div>

                        <!-- Matches List Panel -->
                        <div class="space-y-3">
                            <h4 class="text-sm font-bold text-gray-400">চলমান লাইভ ম্যাচসমূহ</h4>
                            
                            <?php if (empty($data['matches'])): ?>
                                <p class="text-center text-xs py-10 bg-black/10 rounded text-gray-500">কোনো লাইভ ম্যাচ যুক্ত করা হয়নি।</p>
                            <?php else: ?>
                                <div class="overflow-x-auto">
                                    <table class="w-full text-left border-collapse">
                                        <thead>
                                            <tr class="border-b border-green-950 text-xs text-gray-400">
                                                <th class="p-3">বিবরণ / টুর্নামেন্ট</th>
                                                <th class="p-3">দল ১ বনাম দল ২</th>
                                                <th class="p-3">লিংক / টাইপ</th>
                                                <th class="p-3 text-right">অ্যাকশন</th>
                                            </tr>
                                        </thead>
                                        <tbody class="divide-y divide-green-950 text-xs">
                                            <?php foreach ($data['matches'] as $idx => $match): ?>
                                                <tr class="hover:bg-emerald-950/20">
                                                    <td class="p-3">
                                                        <div class="font-bold text-white"><?php echo htmlspecialchars($match['tournament']); ?></div>
                                                        <div class="text-[10px] text-gray-400 mt-0.5">
                                                            <span class="bg-emerald-950 border border-emerald-900 rounded px-1 text-emerald-400 mr-1"><?php echo htmlspecialchars($match['category']); ?></span>
                                                            <span class="text-red-400 font-bold"><?php echo htmlspecialchars($match['status']); ?></span>
                                                        </div>
                                                    </td>
                                                    <td class="p-3">
                                                        <div class="flex items-center gap-2">
                                                            <?php if (!empty($match['team1LogoUrl'])): ?>
                                                                <img src="<?php echo htmlspecialchars($match['team1LogoUrl']); ?>" class="w-4 h-4 object-contain">
                                                            <?php endif; ?>
                                                            <span><?php echo htmlspecialchars($match['team1Name']); ?></span>
                                                            <span class="text-emerald-500 font-bold">vs</span>
                                                            <?php if (!empty($match['team2LogoUrl'])): ?>
                                                                <img src="<?php echo htmlspecialchars($match['team2LogoUrl']); ?>" class="w-4 h-4 object-contain">
                                                            <?php endif; ?>
                                                            <span><?php echo htmlspecialchars($match['team2Name']); ?></span>
                                                        </div>
                                                    </td>
                                                    <td class="p-3 text-[10px] text-gray-400 max-w-xs truncate" title="<?php echo htmlspecialchars($match['streamUrl']); ?>">
                                                        <?php echo htmlspecialchars($match['streamUrl']); ?>
                                                    </td>
                                                    <td class="p-3 text-right">
                                                        <a href="?delete_match=<?php echo $idx; ?>" onclick="return confirm('এই ম্যাচটি ডিলিট করতে চান?')" class="bg-red-950/80 border border-red-900/50 hover:bg-red-900 hover:text-white text-red-300 py-1 px-2.5 rounded font-bold transition">
                                                            <i class="fa-solid fa-trash-can"></i> ডিলিট
                                                        </a>
                                                    </td>
                                                </tr>
                                            <?php endforeach; ?>
                                        </tbody>
                                    </table>
                                </div>
                            <?php endif; ?>
                        </div>
                    </div>


                    <!-- 2. TV CHANNELS SECTION -->
                    <div class="bg-[#07130a] border border-green-900/60 rounded-xl p-6 shadow-xl">
                        <div class="md:flex justify-between items-center mb-6 border-b border-green-950 pb-3">
                            <h3 class="text-lg font-bold text-green-400 flex items-center gap-2">
                                <i class="fa-solid fa-tv text-emerald-400"></i> লাইভ টিভি চ্যানেলসমূহ (Live Channels)
                            </h3>
                            <span class="bg-emerald-950 text-emerald-300 text-xs px-2.5 py-1 rounded border border-emerald-900 mt-2 md:mt-0 inline-block">
                                মোট চ্যানেল: <span class="font-bold"><?php echo count($data['channels']); ?> টি</span>
                            </span>
                        </div>

                        <!-- Add Channel Form -->
                        <div class="mb-8 bg-black/40 border border-green-950/60 rounded-lg p-4">
                            <h4 class="text-sm font-bold text-green-300 mb-4 flex items-center gap-1.5">
                                <i class="fa-solid fa-plus-circle"></i> নতুন চ্যানেল যুক্ত করুন
                            </h4>

                            <form method="POST" action="">
                                <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                                    <div>
                                        <label class="block text-xs text-gray-400 mb-1">ক্যাটাগরি বা দেশ (যেমন: Sports / News / India)</label>
                                        <input type="text" name="categoryName" placeholder="Sports Channels" class="w-full bg-black border border-green-950 rounded p-2 text-sm text-white focus:outline-none focus:border-green-400" required>
                                    </div>
                                    <div>
                                        <label class="block text-xs text-gray-400 mb-1">চ্যানেলের নাম (যেমন: Star Sports 1)</label>
                                        <input type="text" name="channelName" placeholder="Star Sports" class="w-full bg-black border border-green-950 rounded p-2 text-sm text-white focus:outline-none focus:border-green-400" required>
                                    </div>
                                </div>

                                <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                                    <div>
                                        <label class="block text-xs text-gray-400 mb-1">চ্যানেল লোগো লিংক (Logo URL)</label>
                                        <input type="url" name="channelLogoUrl" placeholder="https://example.com/logo.png" class="w-full bg-black border border-green-950 rounded p-2 text-sm text-white focus:outline-none focus:border-green-400">
                                    </div>
                                    <div>
                                        <label class="block text-xs text-gray-400 mb-1">স্ট্রিমিং লিংক (Stream link / HLS .m3u8)</label>
                                        <input type="text" name="streamUrl" placeholder="http://domain.com/live.m3u8" class="w-full bg-black border border-green-950 rounded p-2 text-sm text-white focus:outline-none focus:border-green-400" required>
                                    </div>
                                </div>

                                <button type="submit" name="add_channel" class="bg-emerald-500 hover:bg-emerald-600 text-black font-bold py-2 px-5 rounded text-xs transition flex items-center gap-1">
                                    <i class="fa-solid fa-square-plus"></i> চ্যানেল যুক্ত করুন
                                </button>
                            </form>
                        </div>

                        <!-- Channels List Table -->
                        <div class="space-y-3">
                            <h4 class="text-sm font-bold text-gray-400">চলমান টিভি চ্যানেল তালিকা</h4>
                            
                            <?php if (empty($data['channels'])): ?>
                                <p class="text-center text-xs py-10 bg-black/10 rounded text-gray-500">কোনো টিভি চ্যানেল যুক্ত করা হয়নি।</p>
                            <?php else: ?>
                                <div class="overflow-x-auto">
                                    <table class="w-full text-left border-collapse">
                                        <thead>
                                            <tr class="border-b border-green-950 text-xs text-gray-400">
                                                <th class="p-3">চ্যানেলের নাম</th>
                                                <th class="p-3">ক্যাটাগরি</th>
                                                <th class="p-3">স্ট্রিমিং লিংক</th>
                                                <th class="p-3 text-right">অ্যাকশন</th>
                                            </tr>
                                        </thead>
                                        <tbody class="divide-y divide-green-950 text-xs">
                                            <?php foreach ($data['channels'] as $idx => $chan): ?>
                                                <tr class="hover:bg-emerald-950/20">
                                                    <td class="p-3">
                                                        <div class="flex items-center gap-2.5">
                                                            <?php if (!empty($chan['channelLogoUrl'])): ?>
                                                                <img src="<?php echo htmlspecialchars($chan['channelLogoUrl']); ?>" class="w-6 h-6 object-contain rounded border border-green-950">
                                                            <?php else: ?>
                                                                <div class="w-6 h-6 bg-emerald-950 rounded flex items-center justify-center text-[8px] text-emerald-400"><i class="fa-solid fa-tv"></i></div>
                                                            <?php endif; ?>
                                                            <span class="font-bold text-white"><?php echo htmlspecialchars($chan['channelName']); ?></span>
                                                        </div>
                                                    </td>
                                                    <td class="p-3">
                                                        <span class="bg-emerald-950 border border-green-900 rounded px-1.5 py-0.5 text-[10px] text-emerald-400"><?php echo htmlspecialchars($chan['categoryName']); ?></span>
                                                    </td>
                                                    <td class="p-3 text-[10px] text-gray-400 max-w-xs truncate" title="<?php echo htmlspecialchars($chan['streamUrl']); ?>">
                                                        <?php echo htmlspecialchars($chan['streamUrl']); ?>
                                                    </td>
                                                    <td class="p-3 text-right">
                                                        <a href="?delete_channel=<?php echo $idx; ?>" onclick="return confirm('এই চ্যানেলটি ডিলিট করতে চান?')" class="bg-red-950/80 border border-red-900/50 hover:bg-red-900 hover:text-white text-red-300 py-1 px-2.5 rounded font-bold transition">
                                                            <i class="fa-solid fa-trash-can"></i> ডিলিট
                                                        </a>
                                                    </td>
                                                </tr>
                                            <?php endforeach; ?>
                                        </tbody>
                                    </table>
                                </div>
                            <?php endif; ?>
                        </div>
                    </div>

                </div>

            </div>

        <?php endif; ?>

    </div>

    <!-- Footer -->
    <footer class="bg-black/60 text-center py-6 text-xs text-gray-500 mt-20 border-t border-green-950/60">
        খেলাঘর লাইভ স্পোর্টস কন্ট্রোল প্যানেল &copy; <?php echo date('Y'); ?>. সর্বস্বত্ব সংরক্ষিত।
    </footer>

</body>
</html>
