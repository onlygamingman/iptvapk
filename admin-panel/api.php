<?php
// api.php - Rest API for KhelaGhor Sports App
// Serves JSON structures safely using parameterized inputs (PDO)

header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET");

require_once "config.php";

$action = isset($_GET['action']) ? $_GET['action'] : '';

switch($action) {
    case 'matches':
        try {
            // Prepared Statement to load matches sorted by status and addition date
            $stmt = $pdo->prepare("SELECT * FROM matches ORDER BY status DESC, start_timestamp ASC, added_at DESC");
            $stmt->execute();
            $result = $stmt->fetchAll();
            echo json_encode(["status" => "success", "data" => $result]);
        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode(["status" => "error", "message" => $e->getMessage()]);
        }
        break;

    case 'channels':
        try {
            // Prepared Statement to fetch channel directories
            $stmt = $pdo->prepare("SELECT * FROM channels ORDER BY category_name ASC, added_at DESC");
            $stmt->execute();
            $result = $stmt->fetchAll();
            echo json_encode(["status" => "success", "data" => $result]);
        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode(["status" => "error", "message" => $e->getMessage()]);
        }
        break;

    case 'config':
        try {
            $stmt = $pdo->prepare("SELECT ads_enabled, banner_ad_url, pop_under_url, banner_ad_code, pop_under_code FROM app_config WHERE id = 1 LIMIT 1");
            $stmt->execute();
            $result = $stmt->fetch();
            echo json_encode(["status" => "success", "data" => $result]);
        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode(["status" => "error", "message" => $e->getMessage()]);
        }
        break;

    default:
        http_response_code(400);
        echo json_encode(["status" => "error", "message" => "Invalid API Action requested"]);
        break;
}
?>
