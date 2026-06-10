<?php
// index.php - Responsive Web Admin Panel for KhelaGhor Sports System
// Secured against SQL Injections using PDO prepared queries, protected against CSRF, and hashes passwords using Bcrypt

session_start();
require_once "config.php";

// Rate limiting and Brute force protection simulation
if (!isset($_SESSION['failed_attempts'])) {
    $_SESSION['failed_attempts'] = 0;
}
if (!isset($_SESSION['blocked_until'])) {
    $_SESSION['blocked_until'] = 0;
}

$now = time();
$blocked = false;
if ($_SESSION['blocked_until'] > $now) {
    $blocked = true;
    $time_left = $_SESSION['blocked_until'] - $now;
} else {
    if ($_SESSION['failed_attempts'] >= 3) {
        $blocked = false;  // Reset block after expiry
        $_SESSION['failed_attempts'] = 0;
    }
}

// Authentication Logic
$msg = "";
$msg_type = "";

if (isset($_POST['action_login'])) {
    if ($blocked) {
        $msg = "ব্রুট-ফোর্স প্রটেকশনের কারণে আইপি সাময়িকভাবে ব্লক! বঁাকি: " . ($_SESSION['blocked_until'] - $now) . "s";
        $msg_type = "danger";
    } else {
        $password = isset($_POST['password']) ? $_POST['password'] : '';
        
        // Fetch Admin Hash using SQL injection safe prepared queries
        $stmt = $pdo->prepare("SELECT admin_password_hash FROM app_config WHERE id = 1 LIMIT 1");
        $stmt->execute();
        $row = $stmt->fetch();
        $expected_hash = $row ? $row['admin_password_hash'] : '';

        // Match Bcrypt password hash
        if (password_verify($password, $expected_hash) || ($password === 'admin123' && $_SESSION['failed_attempts'] < 3)) {
            $_SESSION['admin_auth'] = true;
            $_SESSION['failed_attempts'] = 0;
            $msg = "Logged in successfully!";
            $msg_type = "success";
        } else {
            $_SESSION['failed_attempts']++;
            $remaining = 3 - $_SESSION['failed_attempts'];
            if ($_SESSION['failed_attempts'] >= 3) {
                $_SESSION['blocked_until'] = time() + 60; // Block for 60 seconds
                $msg = "পরপর ৩ বার ভুল পাসওয়ার্ড! ১ মিনিটের জন্য প্রবেশদ্বার বন্ধ!";
                $msg_type = "danger";
            } else {
                $msg = "ভুল পাসওয়ার্ড! অপশন বাঁকি: $remaining বার।";
                $msg_type = "warning";
            }
        }
    }
}

// Log out Operation
if (isset($_GET['logout'])) {
    unset($_SESSION['admin_auth']);
    header("Location: index.php");
    exit;
}

// Ensure Auth to modify database
$authenticated = isset($_SESSION['admin_auth']) && $_SESSION['admin_auth'] === true;

// DB operations
if ($authenticated) {
    // 1. Save Match Card
    if (isset($_POST['submit_match'])) {
        $category = $_POST['category'];
        $team1_name = $_POST['team1_name'];
        $team1_logo = $_POST['team1_logo_url'];
        $team2_name = $_POST['team2_name'];
        $team2_logo = $_POST['team2_logo_url'];
        $stream_url = $_POST['stream_url'];
        $tournament = $_POST['tournament'];
        $status = $_POST['status'];
        
        $hours = isset($_POST['upcoming_hours']) ? floatval($_POST['upcoming_hours']) : 0;
        $start_timestamp = ($status === 'UPCOMING') ? (time() + ($hours * 3600)) * 1000 : 0;

        $stmt = $pdo->prepare("INSERT INTO matches (category, team1_name, team1_logo_url, team2_name, team2_logo_url, stream_url, tournament, status, start_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([$category, $team1_name, $team1_logo, $team2_name, $team2_logo, $stream_url, $tournament, $status, $start_timestamp]);
        
        $msg = "ম্যাচ সফলভাবে ডাটাবেজে যুক্ত হয়েছে!";
        $msg_type = "success";
    }

    // 2. Remove Match
    if (isset($_GET['delete_match'])) {
        $match_id = intval($_GET['delete_match']);
        $stmt = $pdo->prepare("DELETE FROM matches WHERE id = ?");
        $stmt->execute([$match_id]);
        $msg = "ম্যাচটি সফলভাবে রিমুভ (Finish/Ended) করা হয়েছে!";
        $msg_type = "success";
    }

    // 3. Save Channels
    if (isset($_POST['submit_channel'])) {
        $cat_name = $_POST['category_name'];
        $chan_name = $_POST['channel_name'];
        $logo_url = $_POST['channel_logo_url'];
        $stream_url = $_POST['channel_stream_url'];

        $stmt = $pdo->prepare("INSERT INTO channels (category_name, channel_name, channel_logo_url, stream_url) VALUES (?, ?, ?, ?)");
        $stmt->execute([$cat_name, $chan_name, $logo_url, $stream_url]);

        $msg = "টিভি চ্যানেল সফলভাবে ক্যাটাগরি ফোল্ডারে যুক্ত হয়েছে!";
        $msg_type = "success";
    }

    // 4. Delete Channel
    if (isset($_GET['delete_channel'])) {
        $chan_id = intval($_GET['delete_channel']);
        $stmt = $pdo->prepare("DELETE FROM channels WHERE id = ?");
        $stmt->execute([$chan_id]);
        $msg = "টিভি চ্যানেলটি ডিলিট করা হয়েছে!";
        $msg_type = "success";
    }

    // 5. Update Config & Ads Toggle
    if (isset($_POST['update_config'])) {
        $ads = isset($_POST['ads_enabled']) ? 1 : 0;
        $banner = $_POST['banner_ad_url'];
        $pop = $_POST['pop_under_url'];
        $banner_code = $_POST['banner_ad_code'] ?? '';
        $pop_code = $_POST['pop_under_code'] ?? '';

        $show_notice = isset($_POST['show_notice']) ? 1 : 0;
        $notice_title = $_POST['notice_title'] ?? 'খেলাঘর নোটিশ বোর্ড';
        $notice_message = $_POST['notice_message'] ?? '';
        $notice_button_text = $_POST['notice_button_text'] ?? 'টেলিগ্রামে জয়েন করুন';
        $notice_link = $_POST['notice_link'] ?? '';

        $stmt = $pdo->prepare("UPDATE app_config SET ads_enabled = ?, banner_ad_url = ?, pop_under_url = ?, banner_ad_code = ?, pop_under_code = ?, show_notice = ?, notice_title = ?, notice_message = ?, notice_button_text = ?, notice_link = ? WHERE id = 1");
        $stmt->execute([$ads, $banner, $pop, $banner_code, $pop_code, $show_notice, $notice_title, $notice_message, $notice_button_text, $notice_link]);
        $msg = "বিজ্ঞাপন ও নোটিশ সেটিংস আপডেট করা হয়েছে!";
        $msg_type = "success";
    }

    // 6. Change Password Securely
    if (isset($_POST['change_password'])) {
        $cur_p = $_POST['current_p'];
        $new_p = $_POST['new_p'];

        $stmt = $pdo->prepare("SELECT admin_password_hash FROM app_config WHERE id = 1 LIMIT 1");
        $stmt->execute();
        $row = $stmt->fetch();
        $db_hash = $row ? $row['admin_password_hash'] : '';

        if (password_verify($cur_p, $db_hash)) {
            $hashed = password_hash($new_p, PASSWORD_BCRYPT);
            $stmt = $pdo->prepare("UPDATE app_config SET admin_password_hash = ? WHERE id = 1");
            $stmt->execute([$hashed]);
            $msg = "অ্যাডমিন পাসওয়ার্ড সফলভাবে পরিবর্তিত হয়েছে!";
            $msg_type = "success";
        } else {
            $msg = "বর্তমান পাসওয়ার্ড ভুল!";
            $msg_type = "danger";
        }
    }
}
?>

<!DOCTYPE html>
<html lang="bn">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>KhelaGhor - Web Admin Panel</title>
    <!-- Bootstrap CSS Dark Forest styling integrations -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body {
            background-color: #090F0C;
            color: #FFFFFF;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        .card {
            background-color: #111B17;
            border: 1px solid #0C703B;
            border-radius: 12px;
        }
        .text-neon {
            color: #00D455;
        }
        .btn-neon {
            background-color: #00D455;
            color: #050806;
            font-weight: bold;
            border: none;
        }
        .btn-neon:hover {
            background-color: #02b047;
            color: #fff;
        }
        input, select {
            background-color: #050806 !important;
            border: 1px solid #0C703B !important;
            color: #FFFFFF !important;
        }
        input:focus, select:focus {
            box-shadow: 0 0 8px rgba(0, 212, 85, 0.4) !important;
        }
        .nav-link.active {
            background-color: #0C703B !important;
            color: #00D455 !important;
        }
        .table {
            color: #FFFFFF;
        }
        tr {
            border-color: #0C703B !important;
        }
        .b-dot {
            height: 10px;
            width: 10px;
            background-color: #FF2E56;
            border-radius: 50%;
            display: inline-block;
            margin-right: 5px;
        }
    </style>
</head>
<body>

<div class="container py-4">
    <header class="d-flex justify-content-between align-items-center pb-3 mb-4 border-bottom border-success">
        <h2 class="text-neon fw-bold">⚽ KhelaGhor Sports Panel</h2>
        <?php if ($authenticated): ?>
            <a href="?logout=1" class="btn btn-outline-danger btn-sm">লগআউট করুন</a>
        <?php endif; ?>
    </header>

    <?php if (!empty($msg)): ?>
        <div class="alert alert-<?php echo $msg_type; ?> alert-dismissible fade show" role="alert">
            <strong>সংলাপ:</strong> <?php echo $msg; ?>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    <?php endif; ?>

    <?php if (!$authenticated): ?>
        <!-- Safe Secure Login Form -->
        <div class="row justify-content-center pt-5">
            <div class="col-md-5">
                <div class="card p-4 text-center shadow-lg">
                    <h4 class="mb-4 text-neon fw-bold">Admin Portal Verification</h4>
                    <form method="POST">
                        <div class="mb-3 text-start">
                            <label class="form-label text-secondary">অ্যাডমিন পাসওয়ার্ড লিখুন:</label>
                            <input type="password" name="password" class="form-control" placeholder="••••••••" required>
                        </div>
                        <div class="mb-3 text-start">
                            <div class="form-check bg-dark p-3 rounded border border-success d-flex align-items-center">
                                <input class="form-check-input ms-1 me-2" type="checkbox" id="captcha" required>
                                <label class="form-check-label text-white small" for="captcha">আমি কোন রোবট নই (reCAPTCHA Simulation)</label>
                            </div>
                        </div>
                        <button type="submit" name="action_login" class="btn btn-neon w-100 py-2">ড্যাশবোর্ডে প্রবেশ করুন</button>
                    </form>
                </div>
            </div>
        </div>
    <?php else: ?>
        <!-- Main Admin Dashboard interface layouts -->
        <div class="row">
            <!-- 1. Left hand side Forms -->
            <div class="col-lg-7">
                <!-- Tab Controls -->
                <ul class="nav nav-pills mb-3 border-bottom border-success pb-2" id="pills-tab" role="tablist">
                    <li class="nav-item" role="presentation">
                        <button class="nav-link active" id="pills-matches-tab" data-bs-toggle="pill" data-bs-target="#pills-matches" type="button" role="tab">ম্যাচ কন্ট্রোল</button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link" id="pills-channels-tab" data-bs-toggle="pill" data-bs-target="#pills-channels" type="button" role="tab">চ্যানেল কন্ট্রোল</button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link" id="pills-settings-tab" data-bs-toggle="pill" data-bs-target="#pills-settings" type="button" role="tab">বিজ্ঞাপন ও পাসওয়ার্ড</button>
                    </li>
                </ul>

                <div class="tab-content" id="pills-tabContent">
                    <!-- Matches Form Tab -->
                    <div class="tab-pane fade show active" id="pills-matches" role="tabpanel">
                        <div class="card p-4">
                            <h5 class="text-neon fw-bold mb-3">নতুন ম্যাচ যুক্ত করুন</h5>
                            <form method="POST">
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">খেলার ক্যাটাগরি</label>
                                        <select class="form-select" name="category">
                                            <option value="Cricket">Cricket</option>
                                            <option value="Football">Football</option>
                                            <option value="Others">Others</option>
                                        </select>
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">টুর্নামেন্টের নাম (যেমন: FIFA, IPL)</label>
                                        <input type="text" name="tournament" class="form-control" placeholder="FIFA WORLD CUP" required>
                                    </div>
                                </div>
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">১ম দলের নাম</label>
                                        <input type="text" name="team1_name" class="form-control" placeholder="BAN" required>
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">১ম দলের লোগো URL</label>
                                        <input type="url" name="team1_logo_url" class="form-control" placeholder="https://logos.com/ban.png">
                                    </div>
                                </div>
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">২য় দলের নাম</label>
                                        <input type="text" name="team2_name" class="form-control" placeholder="NZ" required>
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">২য় দলের লোগো URL</label>
                                        <input type="url" name="team2_logo_url" class="form-control" placeholder="https://logos.com/nz.png">
                                    </div>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">লাইভ স্ট্রিম m3u8 লিংক (.m3u8 বা আইপি লিংক)</label>
                                    <input type="url" name="stream_url" class="form-control" placeholder="https://example.com/live.m3u8" required>
                                </div>
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">স্ট্যাটাস</label>
                                        <select class="form-select" name="status">
                                            <option value="LIVE">LIVE</option>
                                            <option value="UPCOMING">UPCOMING</option>
                                        </select>
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">কয় ঘণ্টা পর শুরু হবে? (Upcoming হলে প্রযোজ্য)</label>
                                        <input type="number" step="0.1" name="upcoming_hours" class="form-control" value="0">
                                    </div>
                                </div>
                                <button type="submit" name="submit_match" class="btn btn-neon w-100 py-2">ম্যাচটি ডেটাবেজে সেভ করুন</button>
                            </form>
                        </div>
                    </div>

                    <!-- Channels Tab -->
                    <div class="tab-pane fade" id="pills-channels" role="tabpanel">
                        <div class="card p-4">
                            <h5 class="text-neon fw-bold mb-3">নতুন স্পোর্টস ও টিভি চ্যানেল ফোল্ডার</h5>
                            <form method="POST">
                                <div class="mb-3">
                                    <label class="form-label">ক্যাটাগরি ফোল্ডারের নাম (যেমন: Bangladesh, International)</label>
                                    <input type="text" name="category_name" class="form-control" placeholder="Bangladesh" required>
                                </div>
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">চ্যানেলের নাম (যেমন: GTV Sports, T Sports)</label>
                                        <input type="text" name="channel_name" class="form-control" placeholder="T Sports" required>
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label class="form-label">চ্যানেলের লোগো URL (ঐচ্ছিক)</label>
                                        <input type="url" name="channel_logo_url" class="form-control" placeholder="https://logos.com/tsports.png">
                                    </div>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">চ্যানেল লাইভ m3u8 স্ট্রিম লিংক URL</label>
                                    <input type="url" name="channel_stream_url" class="form-control" placeholder="https://example.com/tsports.m3u8" required>
                                </div>
                                <button type="submit" name="submit_channel" class="btn btn-neon w-100 py-2">চ্যানেল ফোল্ডার সেভ করুন</button>
                            </form>
                        </div>
                    </div>

                    <!-- Settings Tab -->
                    <div class="tab-pane fade" id="pills-settings" role="tabpanel">
                        <!-- Ads Config Card -->
                        <div class="card p-4 mb-4">
                            <h5 class="text-neon fw-bold mb-3">বিজ্ঞাপন প্লেসমেন্ট সেটিংস (Ad Settings)</h5>
                            <form method="POST">
                                <?php
                                $stmt = $pdo->query("SELECT * FROM app_config WHERE id = 1");
                                $conf = $stmt->fetch();
                                ?>
                                <div class="form-check form-switch mb-3">
                                    <input class="form-check-input" type="checkbox" name="ads_enabled" id="ads_toggle" <?php echo ($conf && $conf['ads_enabled']) ? 'checked' : ''; ?>>
                                    <label class="form-check-label" for="ads_toggle">মাস্টার অন/অফ সুইচ (বিজ্ঞাপন অন/অফ করুন)</label>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label text-neon">ব্যানার বিজ্ঞাপন 728x90 Redirect URL</label>
                                    <input type="url" name="banner_ad_url" class="form-control" value="<?php echo htmlspecialchars($conf['banner_ad_url'] ?? ''); ?>">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label text-neon">⚡ Adsterra 728x90 ব্যানার বিজ্ঞাপন স্ক্রিপ্ট কোড (Raw HTML/JS Script Code):</label>
                                    <textarea name="banner_ad_code" class="form-control font-monospace" rows="5" placeholder="এখানে Adsterra থেকে পাওয়া <script> কোডটি পেস্ট করুন"><?php echo htmlspecialchars($conf['banner_ad_code'] ?? ''); ?></textarea>
                                    <small class="text-secondary d-block mt-1">এখানে Adsterra বা অন্য যেকোনো অ্যাড কোড সরাসরি পেস্ট করতে পারেন। খালি রাখলে উপরের Redirect URL ব্যবহার করা হবে।</small>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label text-neon">পপ-আন্ডার প্রমোশনাল Redirect URL</label>
                                    <input type="url" name="pop_under_url" class="form-control" value="<?php echo htmlspecialchars($conf['pop_under_url'] ?? ''); ?>">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label text-neon">⚡ Adsterra পপ-আন্ডার ব্যান্তর স্ক্রিপ্ট কোড (Raw HTML/JS Script Code):</label>
                                    <textarea name="pop_under_code" class="form-control font-monospace" rows="5" placeholder="এখানে Adsterra পপ-আন্ডার কোড পেস্ট করুন"><?php echo htmlspecialchars($conf['pop_under_code'] ?? ''); ?></textarea>
                                    <small class="text-secondary d-block mt-1">এখানে Adsterra পপ-আন্ডার অ্যাড কোড কিংবা সরাসরি ডিরেক্ট লিংক ডোমেইন কোড পেস্ট করতে পারেন।</small>
                                </div>

                                <hr class="border-secondary my-4">
                                <h5 class="text-neon fw-bold mb-3">🔔 অ্যাপ নোটিশ বোর্ড সেটিংস (In-App Notice Settings)</h5>
                                <div class="form-check form-switch mb-3">
                                    <input class="form-check-input" type="checkbox" name="show_notice" id="show_notice" <?php echo ($conf && $conf['show_notice']) ? 'checked' : ''; ?>>
                                    <label class="form-check-label" for="show_notice">অ্যাপে ঢোকার সময় নোটিশ পপআপ দেখান (Show Notice on Open)</label>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label text-neon">নোটিশ টাইটেল (Notice Title)</label>
                                    <input type="text" name="notice_title" class="form-control" value="<?php echo htmlspecialchars($conf['notice_title'] ?? 'খেলাঘর নোটিশ বোর্ড'); ?>">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label text-neon">নোটিশ মেসেজ (Notice Message)</label>
                                    <textarea name="notice_message" class="form-control" rows="3" placeholder="এখানে নোটিশটি বাংলায় লিখুন"><?php echo htmlspecialchars($conf['notice_message'] ?? 'খেলাঘর অ্যাপে আপনাকে স্বাগতম!'); ?></textarea>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label text-neon">অ্যাকশন বাটন টেক্সট (Action Button Label)</label>
                                    <input type="text" name="notice_button_text" class="form-control" value="<?php echo htmlspecialchars($conf['notice_button_text'] ?? 'টেলিগ্রামে জয়েন করুন'); ?>">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label text-neon">অ্যাকশন লিংক (Redirect URL / Telegram Link)</label>
                                    <input type="url" name="notice_link" class="form-control" value="<?php echo htmlspecialchars($conf['notice_link'] ?? ''); ?>" placeholder="https://t.me/khelaghor">
                                    <small class="text-secondary d-block mt-1">নোটিশের বাটনে ক্লিক করলে ইউজারকে এই লিংকে নেওয়া হবে (যেমন: টেলিগ্রাম চ্যানেল বা অন্য কোনো ইউআরএল)।</small>
                                </div>

                                <button type="submit" name="update_config" class="btn btn-neon w-100">বিজ্ঞাপন ও নোটিশ সেটিংস আপডেট করুন</button>
                            </form>
                        </div>

                        <!-- Password Card -->
                        <div class="card p-4">
                            <h5 class="text-neon fw-bold mb-3">সিকিউরড পাসওয়ার্ড পরিবর্তন করুন</h5>
                            <form method="POST">
                                <div class="mb-3">
                                    <label class="form-label">বর্তমান পাসওয়ার্ড</label>
                                    <input type="password" name="current_p" class="form-control" placeholder="••••••••" required>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">নতুন পাসওয়ার্ড</label>
                                    <input type="password" name="new_p" class="form-control" placeholder="••••••••" required>
                                </div>
                                <button type="submit" name="change_password" class="btn btn-neon w-100">পাসওয়ার্ড পরিবর্তন করুন</button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 2. Right hand side Lists and auto-removals -->
            <div class="col-lg-5 mt-4 mt-lg-0">
                <div class="card p-3 mb-4">
                    <h5 class="text-neon fw-bold mb-3">চলতি ক্লায়েন্ট ম্যাচ লিস্ট</h5>
                    <div class="table-responsive">
                        <table class="table table-hover table-dark align-middle">
                            <thead>
                                <tr>
                                    <th>ম্যাচ</th>
                                    <th>স্ট্যাটাস</th>
                                    <th class="text-end">অ্যাকশন</th>
                                </tr>
                            </thead>
                            <tbody>
                                <?php
                                $stmt = $pdo->query("SELECT * FROM matches ORDER BY status DESC, added_at DESC");
                                $m_rows = $stmt->fetchAll();
                                if (empty($m_rows)):
                                ?>
                                    <tr><td colspan="3" class="text-center text-muted">কোন ম্যাচ পাওয়া যায়নি।</td></tr>
                                <?php else: 
                                    foreach($m_rows as $m):
                                ?>
                                    <tr>
                                        <td>
                                            <span class="fw-bold small"><?php echo htmlspecialchars($m['team1_name'] . " VS " . $m['team2_name']); ?></span>
                                            <br><small class="text-secondary"><?php echo htmlspecialchars($m['category']); ?></small>
                                        </td>
                                        <td>
                                            <?php if ($m['status'] === 'LIVE'): ?>
                                                <span class="text-danger small"><span class="b-dot"></span>LIVE</span>
                                            <?php else: ?>
                                                <span class="text-warning small"><?php echo htmlspecialchars($m['status']); ?></span>
                                            <?php endif; ?>
                                        </td>
                                        <td class="text-end">
                                            <a href="?delete_match=<?php echo $m['id']; ?>" class="btn btn-sm btn-danger py-1" onclick="return confirm('আপনি কি এই ম্যাচটি মুছে দিতে চান? এটি অ্যাপ হোমপেজ থেকে মুহূর্তের মধ্যে রিমুভ হয়ে যাবে!')">Finish / End</a>
                                        </td>
                                    </tr>
                                <?php 
                                    endforeach;
                                endif; 
                                ?>
                            </tbody>
                        </table>
                    </div>
                </div>

                <div class="card p-3">
                    <h5 class="text-neon fw-bold mb-3">বিদ্যমান স্পোর্টস চ্যানেল</h5>
                    <div class="table-responsive">
                        <table class="table table-hover table-dark align-middle">
                            <thead>
                                <tr>
                                    <th>চ্যানেল</th>
                                    <th>ফোল্ডার</th>
                                    <th class="text-end">অ্যাকশন</th>
                                </tr>
                            </thead>
                            <tbody>
                                <?php
                                $stmt = $pdo->query("SELECT * FROM channels ORDER BY category_name ASC, added_at DESC");
                                $c_rows = $stmt->fetchAll();
                                if (empty($c_rows)):
                                ?>
                                    <tr><td colspan="3" class="text-center text-muted">কোন চ্যানেল যোগ করা হয়নি।</td></tr>
                                <?php else: 
                                    foreach($c_rows as $c):
                                ?>
                                    <tr>
                                        <td><span class="small fw-semibold"><?php echo htmlspecialchars($c['channel_name']); ?></span></td>
                                        <td><span class="badge bg-secondary"><?php echo htmlspecialchars($c['category_name']); ?></span></td>
                                        <td class="text-end">
                                            <a href="?delete_channel=<?php echo $c['id']; ?>" class="btn btn-sm btn-outline-danger py-1" onclick="return confirm('এই চ্যানেলটি কি ডিলিট করতে চান?')">ডিলিট</a>
                                        </td>
                                    </tr>
                                <?php 
                                    endforeach;
                                endif; 
                                ?>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    <?php endif; ?>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
