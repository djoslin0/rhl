var JavaPackages = new JavaImporter(
	Packages.ray.rml.Vector3f,
	Packages.java.awt.Color
	);

with (JavaPackages) {
	var ambientColor = new Color(.3, .3, .3);
	var diffuseColor = new Color(.75, .75, .75);
	var specularColor = new Color(.75, .7, 1.0);
	var lightDirection = Vector3f.createFrom(-1.8, 1, -1);
	
	var spawnPoint = Vector3f.createFrom(-40, 1.9, -53.5);
	var puckSpawnPoint = Vector3f.createFrom(0, 5, -10);
	

	var terrainEmissive = new Color(.2, .2, .2);
	var terrainSpecular = new Color(.5, .5, .5);

	var goalDistance = 85.0;

	var debug1 = 0.2;
	var debug2 = 1.0;
	var debug3 = 0.2;
	var debug4 = 0.3;
	var debug5 = 0.0;

	// goal size
	var debugPosition = Vector3f.createFrom(79.0, 4.9, -8.0);
	var debugScale = Vector3f.createFrom(1.0, 1.0, 1.0);
	var debugRotation = Vector3f.createFrom(0.0, 0.0, 0.0);
}