import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Application {

    // The window handle
    private long window;
    public static int WINDOW_WIDTH = 1280;
    public static int WINDOW_HEIGHT = 720;
    public static String WINDOW_TITLE = "Batch Renderer";

    public void run() {

        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_TITLE, NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void loop() {
        GL.createCapabilities();

        // Enables openGL debug messages
        GLUtil.setupDebugMessageCallback();

        // Set the clear color
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        //GL30.glViewport(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        Vertex v0 = new Vertex(); v0.setXYZW(-0.5f, 0.5f, 0f, 1.0f); v0.setRGBA(1.0f, 0.0f, 0.0f, 1.0f);
        Vertex v1 = new Vertex(); v1.setXYZW(-0.5f, -0.5f, 0f, 1.0f); v1.setRGBA(0.0f, 1.0f, 0.0f, 1.0f);
        Vertex v2 = new Vertex(); v2.setXYZW( 0.5f,-0.5f, 0f, 1.0f); v2.setRGBA(0.0f, 0.0f, 1.0f, 1.0f);
        Vertex v3 = new Vertex(); v3.setXYZW( 0.5f, 0.5f, 0f, 1.0f); v3.setRGBA(0.0f, 0.0f, 0.0f, 1.0f);
        Vertex[] vertices = new Vertex[] {v0, v1, v2, v3};

        // VBO (Vertex Buffer Object)
        FloatBuffer vboBuffer = BufferUtils.createFloatBuffer(vertices.length * Vertex.elementCount);
        for(int vertex = 0; vertex < vertices.length; vertex++) {
            //vboBuffer.put(vertices[vertex].getElements());
            vboBuffer.put(vertices[vertex].getXYZW());
            vboBuffer.put(vertices[vertex].getRGBA());
        }
        vboBuffer.flip();

        // IBO (Index Buffer Object)
        int[] indices = {
                0, 1, 2,
                2, 3, 0
        };
        IntBuffer iboBuffer = BufferUtils.createIntBuffer(indices.length);
        iboBuffer.put(indices);
        iboBuffer.flip();

        // VAO (Vertex Array Object)
        int vaoID = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoID);

        // Bind Buffer Data TODO: Make Dynamic
        int vboID = GL30.glGenBuffers();
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vboID);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, vboBuffer, GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(0, Vertex.positionElementCount, Vertex.type, false, Vertex.stride, Vertex.positionOffset);
        GL30.glVertexAttribPointer(1, Vertex.colorElementCount, Vertex.type, false, Vertex.stride, Vertex.colorOffset);

        GL30.glBindVertexArray(0);

        int iboID = GL30.glGenBuffers();
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, iboID);
        GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, iboBuffer, GL30.GL_STATIC_DRAW);
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, 0);

        Shader shaderHandler = new Shader();
        shaderHandler.addShader("/shaders/vert.shader", GL30.GL_VERTEX_SHADER);
        shaderHandler.addShader("/shaders/frag.shader", GL30.GL_FRAGMENT_SHADER);
        shaderHandler.bindProgram();

        while ( !glfwWindowShouldClose(window) ) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            // Bind VAO
            GL30.glBindVertexArray(vaoID);
            GL30.glEnableVertexAttribArray(0);
            GL30.glEnableVertexAttribArray(1);
            GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, iboID);

            // Draw the vertices
            GL30.glDrawElements(GL30.GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);

            // Unbind VAO
            GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL30.glDisableVertexAttribArray(0);
            GL30.glDisableVertexAttribArray(1);
            GL30.glBindVertexArray(0);


            glfwSwapBuffers(window); // swap the color buffers
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new Application().run();
    }

}