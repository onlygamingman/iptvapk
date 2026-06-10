<?php
/*
|--------------------------------------------------------------------------
| Live Sports JSON API (api.php)
|--------------------------------------------------------------------------
| This file yields the JSON data representing matches, channels, and configs.
| It serves as the bridge between your web admin panel and your Android App.
| Place this in your hosting root folder (e.g. public_html) along with admin.php.
*/

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");
header('Content-Type: application/json');

$dbFile = 'data.json';

// Initialize default empty data structural template if data.json doesn't exist
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
        'matches' => [
            [
                'category' => 'Cricket',
                'team1Name' => 'Bangladesh',
                'team1LogoUrl' => 'https://upload.wikimedia.org/wikipedia/commons/f/f9/Flag_of_Bangladesh.svg',
                'team2Name' => 'India',
                'team2LogoUrl' => 'https://upload.wikimedia.org/wikipedia/en/4/41/Flag_of_India.svg',
                'streamUrl' => 'http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
                'tournament' => 'T20 World Cup',
                'status' => 'LIVE',
                'startTimeStamp' => time() * 1000
            ]
        ],
        'channels' => [
            [
                'categoryName' => 'Sports Channels',
                'channelName' => 'Test TV 1',
                'channelLogoUrl' => 'https://upload.wikimedia.org/wikipedia/commons/e/e0/Placeholder_no_image.svg',
                'streamUrl' => 'http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'
            ]
        ]
    ];
    file_put_contents($dbFile, json_encode($defaultData, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
}

echo file_get_contents($dbFile);
?>
