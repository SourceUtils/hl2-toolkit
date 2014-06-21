package com.timepath.hl2.io.bsp;

import com.timepath.hl2.io.bsp.lump.Edge;
import com.timepath.hl2.io.bsp.lump.Face;
import com.timepath.hl2.io.bsp.lump.LumpType;
import com.timepath.steam.io.storage.ACF;
import com.timepath.vfs.ZipFS;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class VBSP extends BSP {

    private static final Logger LOG = Logger.getLogger(VBSP.class.getName());

    public VBSP() {}

    public static void main(String... args) throws Exception {
        BSP b = BSP.load(ACF.fromManifest(440).query("tf/maps/ctf_2fort.bsp").openStream());
        LOG.log(Level.INFO, "Revision: {0}", b.getRevision());
        String ents = b.getLump(LumpType.LUMP_ENTITIES);
        //        System.out.println(ents);
        ZipFS z = b.getLump(LumpType.LUMP_PAKFILE);
        LOG.info(z.name);
        b.getLump(LumpType.LUMP_VERTEXES);
    }

    /**
     * @throws java.io.IOException
     * @see @see <a>https://github.com/toji/webgl-source/blob/master/js/source-bsp.js#L567</a>
     */
    public void processToji() throws IOException {
        FloatBuffer bspVertices = getLump(LumpType.LUMP_VERTEXES);
        LOG.log(Level.INFO, "Vertices: {0}", bspVertices.capacity());
        Edge[] bspEdges = getLump(LumpType.LUMP_EDGES);
        LOG.log(Level.INFO, "Edges: {0}", bspEdges.length);
        int[] bspSurfEdges = getLump(LumpType.LUMP_SURFEDGES);
        LOG.log(Level.INFO, "Surfedges: {0}", bspSurfEdges.length);
        Face[] bspFaces = getLump(LumpType.LUMP_FACES);
        LOG.log(Level.INFO, "Faces: {0}", bspFaces.length);
        List<Float> vertices = new LinkedList<>();
        List<Integer> indices = new LinkedList<>();
        int vertexBase = 0;
        int rootPoint = 0;
        int rootVertId = 0;
        for(Face face : bspFaces) {
            int edgeId = face.firstedge;
            Map<Integer, Integer> vertLookupTable = new HashMap<>(0);
            for(int i = 0; i < face.numedges; i++) {
                int surfEdge = bspSurfEdges[edgeId + 1];
                Edge edge = bspEdges[Math.abs(surfEdge)];
                boolean reverse = surfEdge >= 0;
                int vertId;
                int pointB;
                if(i == 0) {
                    rootVertId = edge.v[reverse ? 0 : 1];
                    rootPoint = compileGpuVertex(bspVertices, rootVertId, vertices);
                    vertLookupTable.put(rootVertId, rootPoint);
                    vertId = edge.v[reverse ? 1 : 0];
                    pointB = compileGpuVertex(bspVertices, vertId, vertices);
                    vertLookupTable.put(vertId, pointB);
                } else {
                    vertId = edge.v[reverse ? 0 : 1];
                    if(vertId == rootVertId) {
                        continue;
                    }
                    int pointA;
                    if(vertLookupTable.containsKey(vertId)) {
                        pointA = vertLookupTable.get(vertId);
                    } else {
                        pointA = compileGpuVertex(bspVertices, vertId, vertices);
                        vertLookupTable.put(vertId, pointA);
                    }
                    vertId = edge.v[reverse ? 1 : 0];
                    if(vertId == rootVertId) {
                        continue;
                    }
                    if(vertLookupTable.containsKey(vertId)) {
                        pointB = vertLookupTable.get(vertId);
                    } else {
                        pointB = compileGpuVertex(bspVertices, vertId, vertices);
                        vertLookupTable.put(vertId, pointB);
                    }
                    indices.add(rootPoint - vertexBase);
                    indices.add(pointA - vertexBase);
                    indices.add(pointB - vertexBase);
                }
            }
        }
        done(vertices, indices);
    }

    @SuppressWarnings({ "empty-statement", "StatementWithEmptyBody" })
    private static int compileGpuVertex(FloatBuffer verts, int pos, Collection<Float> vertices) {
        int index = vertices.size() / 3;
        verts.position(pos);
        for(int i = 0; i++ < 3; vertices.add(verts.get())) ;
        return index;
    }

    private void done(List<Float> vertices, List<Integer> indices) {
        if(( indices != null ) && !indices.isEmpty()) {
            indexBuffer = ByteBuffer.allocateDirect(indices.size() * 4).asIntBuffer();
            for(int i : indices) {
                indexBuffer.put(i);
            }
            indexBuffer.flip();
            LOG.log(Level.INFO, "Map: indices {0}, triangles {1}", new Object[] { indices.size(), indices.size() / 3 });
        }
        if(( vertices != null ) && !vertices.isEmpty()) {
            vertexBuffer = ByteBuffer.allocateDirect(vertices.size() * 4).asFloatBuffer();
            for(float v : vertices) {
                vertexBuffer.put(v);
            }
            vertexBuffer.flip();
            LOG.log(Level.INFO, "Map: vertices {0}", new Object[] { vertices.size() });
        }
    }

    /**
     * @throws java.io.IOException
     * @see <a>https://github.com/w23/OpenSource/blob/master/src/BSP.cpp#L222</a>
     */
    @SuppressWarnings({ "empty-statement", "StatementWithEmptyBody" })
    public void processW23() throws IOException {
        FloatBuffer bspVertices = getLump(LumpType.LUMP_VERTEXES);
        LOG.log(Level.INFO, "Vertices: {0}", bspVertices.capacity());
        Edge[] bspEdges = getLump(LumpType.LUMP_EDGES);
        LOG.log(Level.INFO, "Edges: {0}", bspEdges.length);
        int[] bspSurfEdges = getLump(LumpType.LUMP_SURFEDGES);
        LOG.log(Level.INFO, "Surfedges: {0}", bspSurfEdges.length);
        // https://github.com/w23/OpenSource/blob/master/src/BSP.cpp#L253
        for(int i = 0; i < bspSurfEdges.length; i++) {
            bspSurfEdges[i] = bspSurfEdges[i] >= 0 ? bspEdges[bspSurfEdges[i]].v[0] : bspEdges[-bspSurfEdges[i]].v[1];
        }
        Face[] bspFaces = getLump(LumpType.LUMP_FACES);
        LOG.log(Level.INFO, "Faces: {0}", bspFaces.length);
        List<Float> vertices = new LinkedList<>();
        List<Integer> indices = new LinkedList<>();
        for(Face face : bspFaces) {
            int edgeId = face.firstedge;
            // https://github.com/w23/OpenSource/blob/master/src/BSP.cpp#L347
            int index_shift = vertices.size();
            for(int i = 0; i < face.numedges; i++) {
                bspVertices.position(bspSurfEdges[edgeId + i] * 3);
                for(int j = 0; j++ < 3; vertices.add(bspVertices.get())) ;
                if(i >= 2) {
                    indices.add(index_shift);
                    indices.add(( index_shift + i ) - 1);
                    indices.add(index_shift + i);
                }
            }
        }
        done(vertices, indices);
    }

    @Override
    protected void process() throws IOException {
        processBasic();
    }

    @SuppressWarnings({ "empty-statement", "StatementWithEmptyBody" })
    void processBasic() throws IOException {
        List<Float> vertices = new LinkedList<>();
        FloatBuffer bspVertices = getLump(LumpType.LUMP_VERTEXES);
        for(; bspVertices.hasRemaining(); vertices.add(bspVertices.get())) ;
        done(vertices, null);
    }
}
