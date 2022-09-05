<?php

$image_dir = './uploads';

//https://www.php.net/manual/en/function.scandir.php
function myscandir($dir, $exp, $how='name', $desc=0)
{
    $r = array();
    $dh = @opendir($dir);
    if ($dh) {
        while (($fname = readdir($dh)) !== false) {
            if (preg_match($exp, $fname)) {
                $stat = stat("$dir/$fname");
                $r[$fname] = ($how == 'name')? $fname: $stat[$how];
            }
        }
        closedir($dh);
        if ($desc) {
            arsort($r);
        }
        else {
            asort($r);
        }
    }
    return(array_keys($r));
}

$r = myscandir($image_dir, '/.jpg/i', 'ctime', 1);
//print_r($r); 

if ( count($r) == 0)
{
	
	exit();
}

// Avoid caching
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
header("Cache-Control: post-check=0, pre-check=0", false);
header("Pragma: no-cache");
header('Expires: '. gmdate('D, d M Y H:i:s \G\M\T', time() + (60 * 10))); // 10 minutes

?><!DOCTYPE html>
<head>
<html lang="en">
  <meta charset="utf-8">
  <title>Webcam</title>
	<meta http-equiv="cache-control" content="max-age=0" />
	<meta http-equiv="cache-control" content="no-cache" />
	<meta http-equiv="expires" content="0" />
	<meta http-equiv="expires" content="Tue, 01 Jan 1980 1:00:00 GMT" />
	<meta http-equiv="pragma" content="no-cache" />
  <style>
  
 	body { font-family:Arial; }
	
   </style>
   
</head>
<body>
<div class="content">

<?php

$counter = 0;
foreach ($r AS $image)
{
	$file_path = $image_dir."/".$image ;
	
	if ($counter > 12) // delete older files
	{
		unlink($file_path);
		continue;
	}	
	
	echo '<img src="'. $file_path .'" /><br /><br />';
	
	$counter++;
}
?>
</div>	
</body>
</html>
