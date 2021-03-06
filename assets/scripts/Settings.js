var JavaPackages = new JavaImporter(
	Packages.ray.rml.Vector3f,
	Packages.java.awt.Color
	);

with (JavaPackages) {
	var verticalSync = true;
	var fov = 100;

	var ambientColor = new Color(.3, .3, .3);
	var diffuseColor = new Color(.75, .75, .75);
	var specularColor = new Color(.75, .7, 1.0);
	var lightDirection = Vector3f.createFrom(-1.8, 1, -1);
	
	var puckSpawnPoint = Vector3f.createFrom(0, 25, 0);
	
	var terrainEmissive = new Color(.2, .2, .2);
	var terrainSpecular = new Color(.5, .5, .5);

	var goalDistance = 85.0;

	var serverBotCount = 4;
	var localBotCount = 3;
	var matchSeconds = 5 * 60;
	var intermissionSeconds = 5;

	var debug1 = 0.5;
	var debug2 = 1.0;
	var debug3 = 0.5;
	var debug4 = 0.3;
	var debug5 = 0.0;

	var debugPosition = Vector3f.createFrom(70.0, 1.0, -14.0);
	var debugScale = Vector3f.createFrom(1.0, 1.0, 1.0);
	var debugRotation = Vector3f.createFrom(0.0, 0.0, 0.0);
}