uniform mat4 u_MVPMatrix[24];           // An array representing the combined model/view/projection matrices for each sprite
uniform vec3 u_LightPos;
attribute float a_MVPMatrixIndex;     // The index of the MVPMatrix of the particular sprite
attribute vec4 a_Position;          // Per-vertex position information we will pass in.
attribute vec2 a_TexCoordinate;     // Per-vertex texture coordinate information we will pass in
varying vec2 v_TexCoordinate;     // This will be passed into the fragment shader.
uniform vec4 u_Color;
varying vec4 v_Color;
uniform vec3 u_Normal;

void main()                         // The entry point for our vertex shader.
{
    int mvpMatrixIndex = int(a_MVPMatrixIndex);
    v_TexCoordinate = a_TexCoordinate;
    // gl_Position is a special variable used to store the final position.
    // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
    gl_Position = u_MVPMatrix[mvpMatrixIndex] * a_Position;

    //mat4 MVP = u_MVPMatrix[mvpMatrixIndex];
    // Transform the vertex into eye space.
    vec3 modelViewVertex = vec3(u_MVPMatrix[mvpMatrixIndex] * a_Position);
    // Transform the normal's orientation into eye space.
    vec3 modelViewNormal = vec3(u_MVPMatrix[mvpMatrixIndex] * vec4(u_Normal, 0.0));
    // Will be used for attenuation.
    float distance = length(u_LightPos - modelViewVertex);
    // Get a lighting direction vector from the light to the vertex.
    vec3 lightVector = normalize(u_LightPos - modelViewVertex);
    // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
    // pointing in the same direction then it will get max illumination.
    float diffuse = max(dot(modelViewNormal, lightVector), 0.9);
    // Attenuate the light based on distance.
    diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));
    v_Color = u_Color * diffuse ;
}
